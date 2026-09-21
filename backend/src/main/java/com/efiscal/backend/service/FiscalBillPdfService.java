package com.efiscal.backend.service;

import com.efiscal.backend.model.FiscalBillEntity;
import com.efiscal.backend.model.FiscalBillLineEntity;
import com.efiscal.backend.model.FiscalBillPayEntity;
import com.efiscal.backend.model.FiscalBillTaxEntity;
import com.efiscal.backend.model.OrgEntity;
import com.efiscal.backend.model.TaxEntity;
import com.efiscal.backend.repository.FiscalBillLineRepository;
import com.efiscal.backend.repository.FiscalBillPayRepository;
import com.efiscal.backend.repository.FiscalBillRepository;
import com.efiscal.backend.repository.FiscalBillTaxRepository;
import com.efiscal.backend.repository.OrgRepository;
import com.efiscal.backend.repository.TaxRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.render.Box;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FiscalBillPdfService {

    private static final DateTimeFormatter PFR_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final String ROLL80_PAGE_HEIGHT_TOKEN = "{{PAGE_HEIGHT_MM}}";
    private static final float ROLL80_MEASURE_HEIGHT_MM = 4000f;
    private static final float ROLL80_PAGE_MARGIN_MM = 2f;
    private static final float IMAGE_EXPORT_DPI = 200f;
    private static final float JPEG_QUALITY = 0.9f;

    public enum PdfTemplateFormat {
        A4,
        ROLL80
    }

    public enum ImageMedia {
        PNG,
        JPEG
    }

    private final FiscalBillRepository fiscalBillRepository;
    private final FiscalBillLineRepository fiscalBillLineRepository;
    private final FiscalBillTaxRepository fiscalBillTaxRepository;
    private final FiscalBillPayRepository fiscalBillPayRepository;
    private final OrgRepository orgRepository;
    private final TaxRepository taxRepository;
    private final PdfLabelService pdfLabelService;
    private final EsirNumberService esirNumberService;

    public FiscalBillPdfService(
            FiscalBillRepository fiscalBillRepository,
            FiscalBillLineRepository fiscalBillLineRepository,
            FiscalBillTaxRepository fiscalBillTaxRepository,
            FiscalBillPayRepository fiscalBillPayRepository,
            OrgRepository orgRepository,
            TaxRepository taxRepository,
            PdfLabelService pdfLabelService,
            EsirNumberService esirNumberService) {
        this.fiscalBillRepository = fiscalBillRepository;
        this.fiscalBillLineRepository = fiscalBillLineRepository;
        this.fiscalBillTaxRepository = fiscalBillTaxRepository;
        this.fiscalBillPayRepository = fiscalBillPayRepository;
        this.orgRepository = orgRepository;
        this.taxRepository = taxRepository;
        this.pdfLabelService = pdfLabelService;
        this.esirNumberService = esirNumberService;
    }

    public byte[] generateDefaultA4Pdf(Long fiscalBillId) {
        return generatePdf(fiscalBillId, PdfTemplateFormat.A4);
    }

    public String generateHtml(Long fiscalBillId, PdfTemplateFormat format) {
        FiscalBillEntity bill = fiscalBillRepository.findById(fiscalBillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fiscal bill not found"));

        List<FiscalBillLineEntity> lines = fiscalBillLineRepository.findByFiscalbillId(fiscalBillId);
        List<FiscalBillTaxEntity> taxes = fiscalBillTaxRepository.findByFiscalbillId(fiscalBillId);
        List<FiscalBillPayEntity> payments = fiscalBillPayRepository.findByFiscalbillId(fiscalBillId);
        OrgEntity org = bill.getOrgId() != null ? orgRepository.findById(bill.getOrgId()).orElse(null) : null;

        String template = readTemplate(resolveTemplatePath(format));
        String html = renderTemplate(template, bill, lines, taxes, payments, org, format);
        if (format == PdfTemplateFormat.ROLL80) {
            html = applyRoll80PageHeight(html, ROLL80_MEASURE_HEIGHT_MM);
        }
        return html;
    }

    public byte[] generatePdf(Long fiscalBillId, PdfTemplateFormat format) {
        String html = generateHtml(fiscalBillId, format);
        try {
            if (format == PdfTemplateFormat.ROLL80) {
                return renderRoll80Pdf(html);
            }
            return addA4PageNumbersIfNeeded(renderPdf(html));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate PDF: " + ex.getMessage());
        }
    }

    public byte[] generateImage(Long fiscalBillId, PdfTemplateFormat format, ImageMedia media) {
        byte[] pdf = generatePdf(fiscalBillId, format);
        try (PDDocument document = PDDocument.load(pdf)) {
            if (document.getNumberOfPages() < 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Generated PDF has no pages");
            }
            return encodeImage(renderAllPages(document), media == null ? ImageMedia.PNG : media);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate image: " + ex.getMessage());
        }
    }

    private BufferedImage renderAllPages(PDDocument document) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        int pageCount = document.getNumberOfPages();
        if (pageCount == 1) {
            return renderer.renderImageWithDPI(0, IMAGE_EXPORT_DPI, ImageType.RGB);
        }
        int gap = Math.round(IMAGE_EXPORT_DPI * 2f / 25.4f);
        BufferedImage[] pages = new BufferedImage[pageCount];
        int width = 0;
        int height = 0;
        for (int i = 0; i < pageCount; i++) {
            pages[i] = renderer.renderImageWithDPI(i, IMAGE_EXPORT_DPI, ImageType.RGB);
            width = Math.max(width, pages[i].getWidth());
            height += pages[i].getHeight();
            if (i > 0) {
                height += gap;
            }
        }
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = combined.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        int y = 0;
        for (int i = 0; i < pageCount; i++) {
            graphics.drawImage(pages[i], 0, y, null);
            y += pages[i].getHeight();
            if (i < pageCount - 1) {
                y += gap;
            }
        }
        graphics.dispose();
        return combined;
    }

    public ImageMedia parseImageMedia(String mediaValue) {
        if (mediaValue == null || mediaValue.isBlank()) {
            return ImageMedia.PNG;
        }
        return switch (mediaValue.trim().toLowerCase()) {
            case "png" -> ImageMedia.PNG;
            case "jpeg", "jpg" -> ImageMedia.JPEG;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported image media: " + mediaValue + ". Allowed: png, jpeg");
        };
    }

    public String imageFileExtension(ImageMedia media) {
        return media == ImageMedia.JPEG ? "jpeg" : "png";
    }

    public MediaType imageContentType(ImageMedia media) {
        return media == ImageMedia.JPEG ? MediaType.IMAGE_JPEG : MediaType.IMAGE_PNG;
    }

    private byte[] encodeImage(BufferedImage image, ImageMedia media) throws IOException {
        if (media == ImageMedia.JPEG) {
            return encodeJpeg(image);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) {
            throw new IOException("No PNG image writer available");
        }
        return out.toByteArray();
    }

    private byte[] encodeJpeg(BufferedImage image) throws IOException {
        BufferedImage rgb = image;
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgb.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG image writer available");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
        }
        ImageOutputStream ios = ImageIO.createImageOutputStream(out);
        if (ios == null) {
            writer.dispose();
            throw new IOException("Cannot create JPEG output stream");
        }
        try {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            ios.close();
            writer.dispose();
        }
        return out.toByteArray();
    }

    private byte[] renderRoll80Pdf(String html) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = newPdfRenderer(html, out);
            float lastContentBottomPts;
            try (PdfBoxRenderer renderer = builder.buildPdfRenderer()) {
                renderer.layout();
                lastContentBottomPts = resolveLastContentBottomPts(renderer);
                renderer.createPDF();
            }
            return cropRoll80ToLastContent(out.toByteArray(), lastContentBottomPts);
        }
    }

    private float resolveLastContentBottomPts(PdfBoxRenderer renderer) {
        float pageHeightPts = millimetersToPoints(ROLL80_MEASURE_HEIGHT_MM);
        float lastBottom = renderer.getLastContentBottom();
        float usedMm = pointsToMillimeters(pageHeightPts - lastBottom);
        if (usedMm > 40f && usedMm < ROLL80_MEASURE_HEIGHT_MM - 10f) {
            return lastBottom;
        }
        float contentMm = heightMmFromContentBoxes(renderer);
        return Math.max(0f, pageHeightPts - millimetersToPoints(contentMm));
    }

    private byte[] cropRoll80ToLastContent(byte[] pdfBytes, float lastContentBottomPts) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            while (document.getNumberOfPages() > 1) {
                document.removePage(document.getNumberOfPages() - 1);
            }
            PDPage page = document.getPage(0);
            PDRectangle media = page.getMediaBox();
            float pageHeight = media.getHeight();
            float pad = millimetersToPoints(ROLL80_PAGE_MARGIN_MM);
            float minKeep = millimetersToPoints(40f);
            float cropLly = lastContentBottomPts - pad;
            if (!Float.isFinite(cropLly) || cropLly < 0f || cropLly > pageHeight - minKeep) {
                document.save(out);
                return out.toByteArray();
            }
            PDRectangle box = new PDRectangle(
                    media.getLowerLeftX(),
                    cropLly,
                    media.getWidth(),
                    pageHeight - cropLly);
            page.setMediaBox(box);
            page.setCropBox(box);
            page.setBleedBox(box);
            page.setTrimBox(box);
            document.save(out);
            return out.toByteArray();
        }
    }

    private float heightMmFromContentBoxes(PdfBoxRenderer renderer) {
        Box root = renderer.getRootBox();
        if (root == null || root.getChildCount() == 0) {
            return 80f;
        }
        Box body = root.getChild(0);
        int maxBottom = 0;
        for (int i = 0; i < body.getChildCount(); i++) {
            maxBottom = Math.max(maxBottom, maxBoxBottom(body.getChild(i)));
        }
        float contentMm = (maxBottom / 20f) * 25.4f / 96f;
        return contentMm + (ROLL80_PAGE_MARGIN_MM * 2f);
    }

    private int maxBoxBottom(Box box) {
        int bottom = box.getAbsY() + box.getHeight();
        for (int i = 0; i < box.getChildCount(); i++) {
            bottom = Math.max(bottom, maxBoxBottom(box.getChild(i)));
        }
        return bottom;
    }

    private byte[] addA4PageNumbersIfNeeded(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int pageCount = document.getNumberOfPages();
            if (pageCount <= 1) {
                return pdfBytes;
            }
            float fontSize = 9f;
            float rightMargin = millimetersToPoints(12f);
            float bottomMargin = millimetersToPoints(8f);
            PDType1Font font = PDType1Font.HELVETICA;
            for (int i = 0; i < pageCount; i++) {
                PDPage page = document.getPage(i);
                PDRectangle media = page.getMediaBox();
                String label = (i + 1) + " / " + pageCount;
                float textWidth = font.getStringWidth(label) / 1000f * fontSize;
                float x = media.getLowerLeftX() + media.getWidth() - rightMargin - textWidth;
                float y = media.getLowerLeftY() + bottomMargin;
                try (PDPageContentStream stream = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    stream.beginText();
                    stream.setFont(font, fontSize);
                    stream.newLineAtOffset(x, y);
                    stream.showText(label);
                    stream.endText();
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] renderPdf(String html) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            newPdfRenderer(html, out).run();
            return out.toByteArray();
        }
    }

    private PdfRendererBuilder newPdfRenderer(String html, ByteArrayOutputStream out) {
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        registerFonts(builder);
        builder.withHtmlContent(html, null);
        builder.toStream(out);
        return builder;
    }

    private String applyRoll80PageHeight(String html, float heightMm) {
        return html.replace(ROLL80_PAGE_HEIGHT_TOKEN, formatMillimeters(heightMm));
    }

    private static String formatMillimeters(float heightMm) {
        return String.format(Locale.US, "%.2f", heightMm);
    }

    private static float millimetersToPoints(float mm) {
        return mm * 72f / 25.4f;
    }

    private static float pointsToMillimeters(float points) {
        return points * 25.4f / 72f;
    }

    public PdfTemplateFormat parseTemplateFormat(String formatValue) {
        if (formatValue == null || formatValue.isBlank()) {
            return PdfTemplateFormat.A4;
        }
        String normalized = formatValue.trim().toLowerCase();
        return switch (normalized) {
            case "a4", "default", "default-a4" -> PdfTemplateFormat.A4;
            case "roll", "roll80", "roll-80", "receipt" -> PdfTemplateFormat.ROLL80;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported PDF format: " + formatValue + ". Allowed: a4, roll80");
        };
    }

    public String filenameSuffix(PdfTemplateFormat format) {
        return format == PdfTemplateFormat.ROLL80 ? "roll80" : "a4";
    }

    private void registerFonts(PdfRendererBuilder builder) {
        registerFontWithFallback(
                builder,
                "pdf-fonts/Roboto-Regular.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "Roboto",
                400,
                PdfRendererBuilder.FontStyle.NORMAL);
        registerFontWithFallback(
                builder,
                "pdf-fonts/Roboto-Bold.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
                "Roboto",
                700,
                PdfRendererBuilder.FontStyle.NORMAL);
        registerFontWithFallback(
                builder,
                "pdf-fonts/Roboto-Italic.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Oblique.ttf",
                "Roboto",
                400,
                PdfRendererBuilder.FontStyle.ITALIC);
        registerFontWithFallback(
                builder,
                "pdf-fonts/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "DejaVu Sans",
                400,
                PdfRendererBuilder.FontStyle.NORMAL);
        registerFontWithFallback(
                builder,
                "pdf-fonts/DejaVuSans-Bold.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
                "DejaVu Sans",
                700,
                PdfRendererBuilder.FontStyle.NORMAL);
    }

    private void registerFontWithFallback(
            PdfRendererBuilder builder,
            String classpathLocation,
            String fallbackAbsolutePath,
            String family,
            int weight,
            PdfRendererBuilder.FontStyle style) {
        ClassPathResource classPathFont = new ClassPathResource(classpathLocation);
        if (classPathFont.exists()) {
            FSSupplier<InputStream> classpathSupplier = () -> {
                try {
                    return classPathFont.getInputStream();
                } catch (IOException ex) {
                    return null;
                }
            };
            builder.useFont(classpathSupplier, family, weight, style, true);
            return;
        }

        java.io.File fontFile = new java.io.File(fallbackAbsolutePath);
        if (!fontFile.exists()) {
            return;
        }
        FSSupplier<InputStream> filesystemSupplier = () -> {
            try {
                return new java.io.FileInputStream(fontFile);
            } catch (IOException ex) {
                return null;
            }
        };
        builder.useFont(filesystemSupplier, family, weight, style, true);
    }

    private String resolveTemplatePath(PdfTemplateFormat format) {
        if (format == PdfTemplateFormat.ROLL80) {
            return "pdf-templates/default-roll80.html";
        }
        return "pdf-templates/default-a4.html";
    }

    private String renderTemplate(
            String template,
            FiscalBillEntity bill,
            List<FiscalBillLineEntity> lines,
            List<FiscalBillTaxEntity> taxes,
            List<FiscalBillPayEntity> payments,
            OrgEntity org,
            PdfTemplateFormat format) {

        boolean roll = format == PdfTemplateFormat.ROLL80;
        BigDecimal totalTax = taxes.stream()
                .map(FiscalBillTaxEntity::getAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, String> taxNameByLabel = resolveTaxNameByLabel(taxes);
        List<PdfLine> pdfLines = resolvePdfLines(bill, lines);
        AdvanceClosePdfContext advanceClose = resolveAdvanceCloseContext(bill);

        String invoiceType = invoiceTypeLabel(bill.getEfiscalInvoicetype(), roll);
        String transactionType = transactionTypeLabel(bill.getEfiscalTransactiontype(), roll);

        String html = template;
        html = replace(html, "{{BUSINESS_NAME}}", safe(bill.getEfiscalBusinessname()));
        html = replace(html, "{{BUSINESS_LOCATION}}", safe(bill.getEfiscalLocationname()));
        html = replace(html, "{{BUSINESS_DISTRICT}}", safe(bill.getEfiscalDistrict()));
        html = replace(html, "{{BUSINESS_ADDRESS}}", safe(bill.getEfiscalAddress()));
        html = replace(html, "{{BUSINESS_TIN}}", safe(bill.getEfiscalTin()));
        html = replace(html, "{{CUSTOMER_NAME}}", safe(bill.getCustomerName()));
        html = html.replace("{{CUSTOMER_ID_BLOCK}}", renderCustomerIdBlock(bill, roll));
        html = html.replace("{{CUSTOMER_COST_CENTER_BLOCK}}", renderCustomerCostCenterBlock(bill, roll));
        html = html.replace("{{ADVANCE_PAYMENT_DATE_BLOCK}}", renderAdvancePaymentDateBlock(bill, roll));
        html = replace(html, "{{ORDER_ID}}", safe(bill.getOrderId()));
        html = replace(html, "{{INVOICE_TYPE}}", invoiceType);
        html = replace(html, "{{TRANSACTION_TYPE}}", transactionType);
        html = replace(html, "{{SDC_INVOICE_NO}}", safe(bill.getEfiscalSdcInvoiceno()));
        html = replace(html, "{{SDC_DATE_TIME}}", formatPfrDateTime(safe(bill.getEfiscalSdcdatetime()), roll));
        html = replace(html, "{{ESIR_NUMBER}}", esirNumberService.resolveEsirNumber());
        html = replace(html, "{{INVOICE_COUNTER}}", formatInvoiceCounter(bill));
        html = replace(html, "{{TOTAL_AMOUNT}}", formatAmount(bill.getEfiscalTotalamount()));
        html = replace(html, "{{TOTAL_TAX}}", formatAmount(totalTax));
        html = replace(html, "{{TITLE_LINE}}", renderTitleLine(bill.getEfiscalInvoicetype(), roll));
        html = replace(html, "{{LABEL_FISCAL_BILL_END}}", pdfLabelService.label("fiscalBillEnd", roll));
        html = replace(html, "{{LABEL_TIN}}", pdfLabelService.labelWithColon("tin", roll));
        html = replace(html, "{{LABEL_ESIR_NUMBER}}", pdfLabelService.labelWithColon("esirNumber", roll));
        html = replace(html, "{{LABEL_CASHIER}}", pdfLabelService.labelWithColon("cashier", roll));
        html = replace(html, "{{LABEL_CUSTOMER}}", pdfLabelService.labelWithColon("customer", roll));
        html = replace(html, "{{LABEL_ITEMS}}", pdfLabelService.label("items", roll));
        html = replace(html, "{{LABEL_ITEM_NAME}}", pdfLabelService.label("itemName", roll));
        html = replace(html, "{{LABEL_UNIT_PRICE}}", pdfLabelService.label("unitPrice", roll));
        html = replace(html, "{{LABEL_QUANTITY}}", pdfLabelService.label("quantity", roll));
        html = replace(html, "{{LABEL_LINE_TOTAL}}", pdfLabelService.label("lineTotal", roll));
        html = replace(html, "{{LABEL_TAX_MARK}}", pdfLabelService.label("taxMark", roll));
        html = replace(html, "{{LABEL_TAX_NAME}}", pdfLabelService.label("taxName", roll));
        html = replace(html, "{{LABEL_TAX_RATE}}", pdfLabelService.label("taxRate", roll));
        html = replace(html, "{{LABEL_TAX_AMOUNT}}", pdfLabelService.label("taxAmount", roll));
        html = replace(html, "{{LABEL_PAYMENT_METHOD}}", pdfLabelService.label("paymentMethod", roll));
        html = replace(html, "{{LABEL_PAYMENT_AMOUNT}}", pdfLabelService.label("paymentAmount", roll));
        html = replace(html, "{{LABEL_VERIFY_LINK}}", pdfLabelService.label("verifyLink", roll));
        html = replace(html, "{{LABEL_QR_ALT}}", pdfLabelService.label("qrAlt", roll));
        html = replace(html, "{{LABEL_PAID_IN_ADVANCE}}", pdfLabelService.labelWithColon("paidInAdvance", roll));
        html = replace(html, "{{LABEL_VAT_ON_ADVANCE}}", pdfLabelService.labelWithColon("vatOnAdvance", roll));
        html = replace(html, "{{LABEL_TOTAL_AMOUNT}}", pdfLabelService.labelWithColon("totalAmount", roll));
        html = replace(html, "{{LABEL_TOTAL_TAX}}", pdfLabelService.labelWithColon("totalTax", roll));
        html = replace(html, "{{LABEL_PFR_TIME}}", pdfLabelService.labelWithColon("pfrTime", roll));
        html = replace(html, "{{LABEL_PFR_INVOICE_NUMBER}}", pdfLabelService.labelWithColon("pfrInvoiceNumber", roll));
        html = replace(html, "{{LABEL_INVOICE_COUNTER}}", pdfLabelService.labelWithColon("invoiceCounter", roll));
        html = replace(html, "{{LABEL_LAST_ADVANCE_BILL}}", pdfLabelService.labelWithColon("lastAdvanceBill", roll));
        html = replace(html, "{{EFISCAL_LINK}}", safe(bill.getEfiscalLink()));
        html = replace(html, "{{EFISCAL_MRC}}", formatMrcLine(bill.getEfiscalMrc()));
        html = replace(html, "{{CASHIER_NAME}}", extractCashier(bill.getRequestBody()));
        html = html.replace("{{REFERENT_BLOCK}}", renderReferentBlock(bill, roll));
        // QR is used in <img src="..."> — keep as attribute-safe value, not a visible link
        html = html.replace("{{EFISCAL_QR}}", escapeHtml(resolveQrImageSource(bill.getEfiscalQr(), bill.getEfiscalLink())));
        html = html.replace("{{ORG_LOGO_BLOCK}}", renderLogoBlock(org, roll));
        html = html.replace("{{LINE_ITEMS_ROWS}}",
                roll
                        ? renderRollLineRows(pdfLines, isRefund(bill), advanceClose)
                        : renderLineRows(pdfLines, isRefund(bill), advanceClose));
        html = html.replace("{{TAX_ROWS}}", renderTaxRows(taxes, taxNameByLabel, roll));
        html = html.replace("{{PAYMENT_ROWS}}", roll ? renderRollPaymentRows(payments) : renderPaymentRows(payments));
        html = html.replace("{{ADVANCE_CLOSE_ROWS}}", roll ? renderRollAdvanceCloseRows(advanceClose) : "");
        html = html.replace("{{REMAINING_TO_PAY_ROW}}",
                renderRemainingToPayRow(pdfLines, payments, advanceClose, roll));

        html = html.replace("{{ADVERTISEMENT_BLOCK}}", renderAdvertisementBlock(org, advanceClose, roll));

        return html;
    }

    private String renderCustomerIdBlock(FiscalBillEntity bill, boolean roll) {
        String customerId = safe(bill.getCustomerId()).trim();
        if (customerId.isEmpty()) {
            return "";
        }
        if (roll) {
            return "<tr>"
                    + "<td class=\"k\">" + escapeHtml(pdfLabelService.labelWithColon("customerId", true)) + "</td>"
                    + "<td class=\"v\">" + escapeHtml(customerId) + "</td>"
                    + "</tr>";
        }
        return "<div><span class=\"meta-label\">" + escapeHtml(pdfLabelService.labelWithColon("customerId", false))
                + "</span> " + escapeHtml(customerId) + "</div>";
    }

    private String renderCustomerCostCenterBlock(FiscalBillEntity bill, boolean roll) {
        String costCenterId = safe(bill.getCustomerCostCenterId()).trim();
        if (costCenterId.isEmpty()) {
            return "";
        }
        if (roll) {
            return "<tr>"
                    + "<td class=\"k\">" + escapeHtml(pdfLabelService.labelWithColon("optionalBuyerField", true)) + "</td>"
                    + "<td class=\"v\">" + escapeHtml(costCenterId) + "</td>"
                    + "</tr>";
        }
        return "<div><span class=\"meta-label\">" + escapeHtml(pdfLabelService.labelWithColon("optionalBuyerField", false))
                + "</span> " + escapeHtml(costCenterId) + "</div>";
    }

    /** Advance payment moment sent as Tax Authority {@code dateAndTimeOfIssue}; set on Advance Sale bills only. */
    private String renderAdvancePaymentDateBlock(FiscalBillEntity bill, boolean roll) {
        String dateAndTimeOfIssue = formatPfrDateTime(safe(bill.getDateAndTimeOfIssue()), roll);
        if (dateAndTimeOfIssue.isEmpty()) {
            return "";
        }
        if (roll) {
            return "<tr>"
                    + "<td class=\"k\">" + escapeHtml(pdfLabelService.labelWithColon("advancePaymentDate", true)) + "</td>"
                    + "<td class=\"v\">" + escapeHtml(dateAndTimeOfIssue) + "</td>"
                    + "</tr>";
        }
        return "<div><span class=\"meta-label\">" + escapeHtml(pdfLabelService.labelWithColon("advancePaymentDate", false))
                + "</span> " + escapeHtml(dateAndTimeOfIssue) + "</div>";
    }

    private String renderReferentBlock(FiscalBillEntity bill, boolean roll) {
        Long referentId = bill.getReferentFiscalbillId();
        if (referentId == null) {
            return "";
        }
        FiscalBillEntity referent = fiscalBillRepository.findById(referentId).orElse(null);
        if (referent == null) {
            return "";
        }
        String number = safe(referent.getEfiscalSdcInvoiceno()).trim();
        String dateTime = formatPfrDateTime(safe(referent.getEfiscalSdcdatetime()), roll);
        if (number.isEmpty() && dateTime.isEmpty()) {
            return "";
        }
        if (roll) {
            StringBuilder sb = new StringBuilder();
            sb.append("<tr>")
                    .append("<td class=\"k\">").append(escapeHtml(pdfLabelService.labelWithColon("referentNumber", true))).append("</td>")
                    .append("<td class=\"v\">").append(escapeHtml(number)).append("</td>")
                    .append("</tr>");
            sb.append("<tr>")
                    .append("<td class=\"k\">").append(escapeHtml(pdfLabelService.labelWithColon("referentTime", true))).append("</td>")
                    .append("<td class=\"v\">").append(escapeHtml(dateTime)).append("</td>")
                    .append("</tr>");
            return sb.toString();
        }
        return "<div><span class=\"meta-label\">" + escapeHtml(pdfLabelService.labelWithColon("referentNumber", false))
                + "</span> " + escapeHtml(number) + "</div>"
                + "<div><span class=\"meta-label\">" + escapeHtml(pdfLabelService.labelWithColon("referentTime", false))
                + "</span> " + escapeHtml(dateTime) + "</div>";
    }

    private record PdfLine(
            String name,
            String taxLabel,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            String gtin
    ) {}

    private boolean isAdvanceInvoice(FiscalBillEntity bill) {
        return bill.getEfiscalInvoicetype() != null && bill.getEfiscalInvoicetype() == 4;
    }

    private boolean isRefund(FiscalBillEntity bill) {
        return bill.getEfiscalTransactiontype() != null && bill.getEfiscalTransactiontype() == 1;
    }

    private boolean isNormalSale(FiscalBillEntity bill) {
        return bill.getEfiscalInvoicetype() != null && bill.getEfiscalInvoicetype() == 0
                && bill.getEfiscalTransactiontype() != null && bill.getEfiscalTransactiontype() == 0;
    }

    private boolean isAdvanceRefund(FiscalBillEntity bill) {
        return bill.getEfiscalInvoicetype() != null && bill.getEfiscalInvoicetype() == 4
                && bill.getEfiscalTransactiontype() != null && bill.getEfiscalTransactiontype() == 1;
    }

    private boolean isAdvanceSale(FiscalBillEntity bill) {
        return bill.getEfiscalInvoicetype() != null && bill.getEfiscalInvoicetype() == 4
                && bill.getEfiscalTransactiontype() != null && bill.getEfiscalTransactiontype() == 0;
    }

    private record AdvanceClosePdfContext(
            BigDecimal refundTotal,
            BigDecimal refundTax,
            String lastAdvanceInvoiceNo,
            String lastAdvanceDate
    ) {
        boolean hasRefundTotals() {
            return refundTotal != null || refundTax != null;
        }

        boolean hasLastAdvanceBill() {
            return (lastAdvanceInvoiceNo != null && !lastAdvanceInvoiceNo.isBlank())
                    || (lastAdvanceDate != null && !lastAdvanceDate.isBlank());
        }
    }

    /**
     * When a Normal Sale references an Advance Refund (close-advance chain),
     * expose refund totals and the last Advance Sale PFR identity for the PDF.
     */
    private AdvanceClosePdfContext resolveAdvanceCloseContext(FiscalBillEntity bill) {
        if (bill == null || !isNormalSale(bill) || bill.getReferentFiscalbillId() == null) {
            return null;
        }
        FiscalBillEntity refund = fiscalBillRepository.findById(bill.getReferentFiscalbillId()).orElse(null);
        if (refund == null || !isAdvanceRefund(refund)) {
            return null;
        }

        BigDecimal refundTax = fiscalBillTaxRepository.findByFiscalbillId(refund.getFiscalbillId()).stream()
                .map(FiscalBillTaxEntity::getAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FiscalBillEntity lastAdvance = null;
        if (refund.getReferentFiscalbillId() != null) {
            lastAdvance = fiscalBillRepository.findById(refund.getReferentFiscalbillId()).orElse(null);
            if (lastAdvance != null && !isAdvanceSale(lastAdvance)) {
                lastAdvance = null;
            }
        }

        String invoiceNo = lastAdvance != null ? safe(lastAdvance.getEfiscalSdcInvoiceno()).trim() : "";
        String date = lastAdvance != null ? formatPfrDate(safe(lastAdvance.getEfiscalSdcdatetime())) : "";
        return new AdvanceClosePdfContext(refund.getEfiscalTotalamount(), refundTax, invoiceNo, date);
    }

    /**
     * Advance PDFs must show tax-configured advance names (prefix + name + tax mark),
     * grouped by tax label — not raw product names.
     */
    private List<PdfLine> resolvePdfLines(FiscalBillEntity bill, List<FiscalBillLineEntity> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        if (!isAdvanceInvoice(bill)) {
            List<PdfLine> result = new ArrayList<>();
            for (FiscalBillLineEntity line : lines) {
                result.add(new PdfLine(
                        safe(line.getName()),
                        safe(line.getTaxLabel()),
                        line.getQuantity(),
                        line.getUnitPrice(),
                        line.getTotalAmount(),
                        line.getGtin()));
            }
            return result;
        }

        Map<String, BigDecimal> totalsByLabel = new LinkedHashMap<>();
        for (FiscalBillLineEntity line : lines) {
            String label = safe(line.getTaxLabel()).trim();
            if (label.isEmpty()) {
                label = "?";
            }
            BigDecimal amount = line.getTotalAmount() != null ? line.getTotalAmount() : BigDecimal.ZERO;
            totalsByLabel.merge(label, amount, BigDecimal::add);
        }

        Map<String, String> advanceNames = resolveAdvanceDisplayNames(totalsByLabel.keySet());
        List<PdfLine> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : totalsByLabel.entrySet()) {
            String label = entry.getKey();
            BigDecimal total = entry.getValue();
            String name = advanceNames.getOrDefault(label, AdvanceLineNameResolver.format(null, null, label));
            result.add(new PdfLine(name, label, BigDecimal.ONE, total, total, null));
        }
        return result;
    }

    private Map<String, String> resolveAdvanceDisplayNames(Set<String> labels) {
        Map<String, List<TaxEntity>> activeTaxesByLabel = taxRepository.findAllByDeletedAtIsNullAndIsActiveTrue().stream()
                .filter(t -> t.getLabel() != null && !t.getLabel().isBlank())
                .collect(Collectors.groupingBy(t -> t.getLabel().trim().toUpperCase()));

        Map<String, String> resolved = new HashMap<>();
        for (String label : labels) {
            String normalized = label == null ? "" : label.trim().toUpperCase();
            List<TaxEntity> matches = activeTaxesByLabel.getOrDefault(normalized, List.of());
            if (matches.size() == 1) {
                TaxEntity tax = matches.get(0);
                String prefix = tax.getEfiscalAdvanceprefix();
                String advanceName = tax.getEfiscalAdvancename();
                if (prefix != null && !prefix.isBlank() && advanceName != null && !advanceName.isBlank()) {
                    resolved.put(label, AdvanceLineNameResolver.format(prefix, advanceName, label));
                    continue;
                }
            }
            resolved.put(label, AdvanceLineNameResolver.format(null, null, label));
        }
        return resolved;
    }

    private String renderLineRows(List<PdfLine> lines, boolean refund, AdvanceClosePdfContext advanceClose) {
        if (lines == null || lines.isEmpty()) {
            return "<tr><td colspan=\"4\" class=\"muted\">"
                    + escapeHtml(pdfLabelService.label("noLineItems", false)) + "</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        BigDecimal lineTotal = BigDecimal.ZERO;
        for (PdfLine line : lines) {
            StringBuilder nameCellContent = new StringBuilder();
            nameCellContent.append(escapeHtml(safe(line.name())));
            if (line.gtin() != null && !line.gtin().isBlank()) {
                nameCellContent.append("<div class=\"item-gtin\">")
                        .append(escapeHtml(pdfLabelService.labelWithColon("gtin", false)))
                        .append(" ")
                        .append(escapeHtml(line.gtin()))
                        .append("</div>");
            }
            sb.append("<tr>")
                    .append("<td>").append(nameCellContent).append("</td>")
                    .append("<td class=\"num\">").append(formatAmount(line.unitPrice())).append("</td>")
                    .append("<td class=\"num\">").append(formatAmount(line.quantity())).append("</td>")
                    .append("<td class=\"num\">").append(formatLineTotal(line.totalAmount(), refund)).append("</td>")
                    .append("</tr>");
            if (line.totalAmount() != null) {
                lineTotal = lineTotal.add(line.totalAmount());
            }
        }
        sb.append("<tr class=\"line-items-total\">")
                .append("<td colspan=\"3\" class=\"label\">")
                .append(escapeHtml(pdfLabelService.labelWithColon("itemsTotal", false)))
                .append("</td>")
                .append("<td class=\"num value\">").append(formatAmount(lineTotal)).append("</td>")
                .append("</tr>");
        appendAdvanceCloseRows(sb, advanceClose, false);
        return sb.toString();
    }

    private String renderRollLineRows(List<PdfLine> lines, boolean refund, AdvanceClosePdfContext advanceClose) {
        if (lines == null || lines.isEmpty()) {
            return "<tr><td colspan=\"4\" class=\"muted\">"
                    + escapeHtml(pdfLabelService.label("noLineItems", true)) + "</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        for (PdfLine line : lines) {
            String name = safe(line.name());
            String taxLabel = safe(line.taxLabel()).trim();
            String markSuffix = taxLabel.isEmpty() ? "" : "(" + taxLabel + ")";
            if (!taxLabel.isEmpty() && !name.contains(markSuffix)) {
                name = name + " (" + taxLabel + ")";
            }
            sb.append("<tr class=\"item-name\">")
                    .append("<td colspan=\"4\">").append(escapeHtml(name)).append("</td>")
                    .append("</tr>");
            sb.append("<tr class=\"item-vals\">")
                    .append("<td class=\"spacer\"></td>")
                    .append("<td class=\"num\">").append(formatAmount(line.unitPrice())).append("</td>")
                    .append("<td class=\"num\">").append(formatQuantity(line.quantity())).append("</td>")
                    .append("<td class=\"num\">").append(formatLineTotal(line.totalAmount(), refund)).append("</td>")
                    .append("</tr>");
        }
        return sb.toString();
    }

    private void appendAdvanceCloseRows(StringBuilder sb, AdvanceClosePdfContext ctx, boolean roll) {
        if (ctx == null || !ctx.hasRefundTotals()) {
            return;
        }
        sb.append("<tr class=\"advance-close\">")
                .append("<td colspan=\"3\" class=\"label\">")
                .append(escapeHtml(pdfLabelService.labelWithColon("paidInAdvance", roll)))
                .append("</td>")
                .append("<td class=\"num value\">").append(formatAmount(ctx.refundTotal())).append("</td>")
                .append("</tr>");
        sb.append("<tr class=\"advance-close\">")
                .append("<td colspan=\"3\" class=\"label\">")
                .append(escapeHtml(pdfLabelService.labelWithColon("vatOnAdvance", roll)))
                .append("</td>")
                .append("<td class=\"num value\">").append(formatAmount(ctx.refundTax())).append("</td>")
                .append("</tr>");
    }

    private String renderRemainingToPayRow(
            List<PdfLine> lines,
            List<FiscalBillPayEntity> payments,
            AdvanceClosePdfContext ctx,
            boolean roll) {
        if (ctx == null || !ctx.hasRefundTotals()) {
            return "";
        }
        BigDecimal remaining = calculateRemainingToPay(lines, payments, ctx.refundTotal());
        String label = escapeHtml(pdfLabelService.labelWithColon("remainingToPay", roll));
        String value = formatAmount(remaining);
        if (roll) {
            return "<tr class=\"remaining-to-pay\">"
                    + "<td class=\"k\">" + label + "</td>"
                    + "<td class=\"v\">" + value + "</td>"
                    + "</tr>";
        }
        return "<tr class=\"remaining-to-pay\">"
                + "<td class=\"label\">" + label + "</td>"
                + "<td class=\"value\">" + value + "</td>"
                + "</tr>";
    }

    private BigDecimal calculateRemainingToPay(
            List<PdfLine> lines,
            List<FiscalBillPayEntity> payments,
            BigDecimal advanceRefundTotal) {
        BigDecimal lineItemsTotal = sumLineTotals(lines);
        BigDecimal advanceTotal = advanceRefundTotal != null ? advanceRefundTotal : BigDecimal.ZERO;
        if (lineItemsTotal.compareTo(advanceTotal) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal paymentTotal = sumPaymentTotals(payments);
        BigDecimal remaining = lineItemsTotal.subtract(advanceTotal).subtract(paymentTotal);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    private BigDecimal sumLineTotals(List<PdfLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return lines.stream()
                .map(PdfLine::totalAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumPaymentTotals(List<FiscalBillPayEntity> payments) {
        if (payments == null || payments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return payments.stream()
                .map(FiscalBillPayEntity::getAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String renderRollAdvanceCloseRows(AdvanceClosePdfContext ctx) {
        if (ctx == null || !ctx.hasRefundTotals()) {
            return "";
        }
        return "<tr class=\"advance-close\">"
                + "<td class=\"k\">" + escapeHtml(pdfLabelService.labelWithColon("paidInAdvance", true)) + "</td>"
                + "<td class=\"v\">" + formatAmount(ctx.refundTotal()) + "</td>"
                + "</tr>"
                + "<tr class=\"advance-close\">"
                + "<td class=\"k\">" + escapeHtml(pdfLabelService.labelWithColon("vatOnAdvance", true)) + "</td>"
                + "<td class=\"v\">" + formatAmount(ctx.refundTax()) + "</td>"
                + "</tr>";
    }

    private String renderAdvertisementBlock(OrgEntity org, AdvanceClosePdfContext advanceClose, boolean roll) {
        StringBuilder sb = new StringBuilder();
        if (advanceClose != null && advanceClose.hasLastAdvanceBill()) {
            String number = safe(advanceClose.lastAdvanceInvoiceNo()).trim();
            String date = safe(advanceClose.lastAdvanceDate()).trim();
            String value = (number + " " + date).trim();
            sb.append("<div class=\"advertisement-advance\">")
                    .append("<span class=\"ad-label\">")
                    .append(escapeHtml(pdfLabelService.labelWithColon("lastAdvanceBill", roll)))
                    .append("</span>")
                    .append("<span class=\"ad-value\">").append(escapeHtml(value)).append("</span>")
                    .append("</div>");
        }
        if (org != null && org.isAdvertisementEnabled()
                && org.getAdvertisementHtml() != null && !org.getAdvertisementHtml().isBlank()) {
            sb.append(org.getAdvertisementHtml());
        }
        if (sb.isEmpty()) {
            return "";
        }
        return "<div class=\"advertisement\">" + sb + "</div>";
    }

    private String renderLogoBlock(OrgEntity org, boolean roll) {
        if (org == null || org.getLogoImage() == null || org.getLogoImage().isBlank()) {
            return "";
        }
        String raw = org.getLogoImage().trim();
        String lower = raw.toLowerCase();
        if (!lower.startsWith("data:image/")) {
            return "";
        }
        String src = escapeHtml(raw);
        return "<img class=\"org-logo\" src=\"" + src + "\" alt=\""
                + escapeHtml(pdfLabelService.label("orgLogoAlt", roll)) + "\" />";
    }

    private String renderTaxRows(List<FiscalBillTaxEntity> taxes, Map<String, String> taxNameByLabel, boolean roll) {
        if (taxes == null || taxes.isEmpty()) {
            return "<tr><td colspan=\"4\" class=\"muted\">"
                    + escapeHtml(pdfLabelService.label("noTaxItems", roll)) + "</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        for (FiscalBillTaxEntity tax : taxes) {
            String label = safe(tax.getEfiscalTaxlabel());
            String taxName = taxNameByLabel.getOrDefault(label.trim().toUpperCase(), safe(tax.getEfiscalCategoryname()));
            sb.append("<tr>")
                    .append("<td>").append(escapeHtml(label)).append("</td>")
                    .append("<td>").append(escapeHtml(taxName)).append("</td>")
                    .append("<td class=\"num\">").append(formatPercent(tax.getRate())).append("</td>")
                    .append("<td class=\"num\">").append(formatAmount(tax.getAmount())).append("</td>")
                    .append("</tr>");
        }
        return sb.toString();
    }

    private Map<String, String> resolveTaxNameByLabel(List<FiscalBillTaxEntity> billTaxes) {
        Map<String, String> taxNameByLabel = new HashMap<>();
        if (billTaxes == null || billTaxes.isEmpty()) {
            return taxNameByLabel;
        }

        List<TaxEntity> taxes = taxRepository.findAllByDeletedAtIsNullAndIsActiveTrue();
        for (TaxEntity tax : taxes) {
            String label = safe(tax.getLabel()).trim();
            String efiscalTaxname = safe(tax.getEfiscalTaxname()).trim();
            if (label.isEmpty() || efiscalTaxname.isEmpty()) {
                continue;
            }
            taxNameByLabel.putIfAbsent(label.toUpperCase(), efiscalTaxname);
        }
        return taxNameByLabel;
    }

    private String renderPaymentRows(List<FiscalBillPayEntity> payments) {
        if (payments == null || payments.isEmpty()) {
            return "<tr><td colspan=\"2\" class=\"muted\">"
                    + escapeHtml(pdfLabelService.label("noPayments", false)) + "</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        for (FiscalBillPayEntity pay : payments) {
            sb.append("<tr>")
                    .append("<td>").append(escapeHtml(paymentTypeLabel(pay.getPaymentType(), false))).append("</td>")
                    .append("<td class=\"num\">").append(formatAmount(pay.getAmount())).append("</td>")
                    .append("</tr>");
        }
        return sb.toString();
    }

    private String renderRollPaymentRows(List<FiscalBillPayEntity> payments) {
        if (payments == null || payments.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (FiscalBillPayEntity pay : payments) {
            sb.append("<tr>")
                    .append("<td class=\"k\">").append(escapeHtml(paymentTypeLabel(pay.getPaymentType(), true))).append("</td>")
                    .append("<td class=\"v\">").append(formatAmount(pay.getAmount())).append("</td>")
                    .append("</tr>");
        }
        return sb.toString();
    }

    private String paymentTypeLabel(Integer paymentType) {
        return paymentTypeLabel(paymentType, false);
    }

    private String paymentTypeLabel(Integer paymentType, boolean roll) {
        if (paymentType == null) {
            return pdfLabelService.label("unknown", roll);
        }
        String key = switch (paymentType) {
            case 0 -> "payOther";
            case 1 -> "payCash";
            case 2 -> "payCard";
            case 3 -> "payCheck";
            case 4 -> "payWire";
            case 5 -> "payVoucher";
            case 6 -> "payMobile";
            default -> "unknown";
        };
        return pdfLabelService.label(key, roll);
    }

    private String renderTitleLine(Integer invoiceType, boolean roll) {
        boolean notFiscalBill = invoiceType != null && invoiceType >= 1 && invoiceType <= 3;
        String label = pdfLabelService.label(notFiscalBill ? "notFiscalBill" : "fiscalBill", roll);
        int equalsCount;
        if (roll) {
            equalsCount = notFiscalBill ? 8 : 12;
        } else {
            equalsCount = notFiscalBill ? 39 : 44;
        }
        String pad = "=".repeat(equalsCount);
        return pad + " " + label + " " + pad;
    }

    private String invoiceTypeLabel(Integer invoiceType) {
        return invoiceTypeLabel(invoiceType, false);
    }

    private String invoiceTypeLabel(Integer invoiceType, boolean roll) {
        if (invoiceType == null) {
            return pdfLabelService.label("unknown", roll);
        }
        String key = switch (invoiceType) {
            case 0 -> "invoiceNormal";
            case 1 -> "invoiceProforma";
            case 2 -> "invoiceCopy";
            case 3 -> "invoiceTraining";
            case 4 -> "invoiceAdvance";
            default -> "unknown";
        };
        return pdfLabelService.label(key, roll);
    }

    private String transactionTypeLabel(Integer transactionType) {
        return transactionTypeLabel(transactionType, false);
    }

    private String transactionTypeLabel(Integer transactionType, boolean roll) {
        if (transactionType == null) {
            return pdfLabelService.label("unknown", roll);
        }
        return pdfLabelService.label(transactionType == 1 ? "transactionRefund" : "transactionSale", roll);
    }

    private String replace(String input, String key, String value) {
        return input.replace(key, escapeHtml(value));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String resolveQrImageSource(String efiscalQr, String efiscalLink) {
        String qr = safe(efiscalQr).trim();
        if (!qr.isEmpty()) {
            String lower = qr.toLowerCase();
            if (lower.startsWith("data:image/")) {
                return qr;
            }
            String imageDataUri = toDataUriFromBase64Image(qr);
            if (!imageDataUri.isEmpty()) {
                return imageDataUri;
            }
        }

        String payload = safe(efiscalLink).trim();
        if (payload.isEmpty()) {
            return "";
        }
        return buildQrPngDataUri(payload);
    }

    private String toDataUriFromBase64Image(String base64Value) {
        String compact = base64Value.replaceAll("\\s+", "");
        if (compact.isEmpty()) {
            return "";
        }
        try {
            byte[] imageBytes = Base64.getMimeDecoder().decode(compact);
            String mimeType = detectImageMimeType(imageBytes);
            if (mimeType.isEmpty()) {
                return "";
            }
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private String detectImageMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return "";
        }
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "image/png";
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "image/gif";
        }
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return "";
    }

    private String buildQrPngDataUri(String payload) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 180, 180, hints);
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (WriterException | IOException ex) {
            return "";
        }
    }

    private String formatAmount(BigDecimal value) {
        return formatDecimal(value, 2);
    }

    private String formatLineTotal(BigDecimal value, boolean refund) {
        String formatted = formatAmount(value);
        return refund ? "-" + formatted : formatted;
    }

    private String formatQuantity(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        // Receipt printers typically show whole quantities without trailing decimals.
        if (value.stripTrailingZeros().scale() <= 0) {
            return formatDecimal(value, 0);
        }
        return formatDecimal(value, 3);
    }

    private String formatPercent(BigDecimal value) {
        return formatDecimal(value, 2) + "%";
    }

    private String formatDecimal(BigDecimal value, int scale) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        String decimalPattern = scale <= 0 ? "" : "." + "0".repeat(scale);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0" + decimalPattern, symbols);
        formatter.setRoundingMode(java.math.RoundingMode.HALF_UP);
        return formatter.format(safeValue);
    }

    private String formatInvoiceCounter(FiscalBillEntity bill) {
        String counter = safe(bill.getEfiscalInvoicecounter()).trim();
        String extension = safe(bill.getEfiscalInvoicecounterext()).trim();
        if (counter.isEmpty() && extension.isEmpty()) {
            return "";
        }
        if (extension.isEmpty()) {
            return counter;
        }
        if (counter.isEmpty()) {
            return extension;
        }
        return counter;
    }

    private String formatMrcLine(String mrc) {
        String value = safe(mrc).trim();
        return value.isEmpty() ? "" : value;
    }

    private String extractCashier(String requestBody) {
        String body = safe(requestBody);
        if (body.isBlank()) {
            return "";
        }
        // request_body is JSON submitted to Tax Authority; cashier is optional.
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"cashier\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String formatPfrDate(String value) {
        return formatTemporal(value, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private String formatPfrDateTime(String value) {
        return formatPfrDateTime(value, false);
    }

    private String formatPfrDateTime(String value, boolean roll) {
        DateTimeFormatter displayFormat = roll
                ? DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm:ss")
                : PFR_DISPLAY_FORMAT;
        return formatTemporal(value, displayFormat);
    }

    private String formatTemporal(String value, DateTimeFormatter displayFormat) {
        String input = safe(value).trim();
        if (input.isEmpty()) {
            return "";
        }
        try {
            return OffsetDateTime.parse(input).format(displayFormat);
        } catch (DateTimeParseException ignored) {
            // try other common ISO variants
        }
        try {
            return ZonedDateTime.parse(input).format(displayFormat);
        } catch (DateTimeParseException ignored) {
            // try local date-time variant
        }
        try {
            return LocalDateTime.parse(input).format(displayFormat);
        } catch (DateTimeParseException ignored) {
            // try instant variant
        }
        try {
            return Instant.parse(input).atOffset(java.time.ZoneOffset.UTC).format(displayFormat);
        } catch (DateTimeParseException ignored) {
            return input;
        }
    }

    private String readTemplate(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "PDF template not found: " + classpathLocation);
        }
        try {
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read PDF template: " + ex.getMessage());
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

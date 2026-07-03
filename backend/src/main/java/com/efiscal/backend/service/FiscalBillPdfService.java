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
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FiscalBillPdfService {

    private static final DateTimeFormatter PFR_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public enum PdfTemplateFormat {
        A4,
        ROLL80
    }

    private final FiscalBillRepository fiscalBillRepository;
    private final FiscalBillLineRepository fiscalBillLineRepository;
    private final FiscalBillTaxRepository fiscalBillTaxRepository;
    private final FiscalBillPayRepository fiscalBillPayRepository;
    private final OrgRepository orgRepository;
    private final TaxRepository taxRepository;

    public FiscalBillPdfService(
            FiscalBillRepository fiscalBillRepository,
            FiscalBillLineRepository fiscalBillLineRepository,
            FiscalBillTaxRepository fiscalBillTaxRepository,
            FiscalBillPayRepository fiscalBillPayRepository,
            OrgRepository orgRepository,
            TaxRepository taxRepository) {
            OrgRepository orgRepository,
            TaxRepository taxRepository) {
        this.fiscalBillRepository = fiscalBillRepository;
        this.fiscalBillLineRepository = fiscalBillLineRepository;
        this.fiscalBillTaxRepository = fiscalBillTaxRepository;
        this.fiscalBillPayRepository = fiscalBillPayRepository;
        this.orgRepository = orgRepository;
        this.taxRepository = taxRepository;
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
        String html = renderTemplate(template, bill, lines, taxes, payments, org);
        return html;
        }

        public byte[] generatePdf(Long fiscalBillId, PdfTemplateFormat format) {
        String html = generateHtml(fiscalBillId, format);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerFonts(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate PDF: " + ex.getMessage());
        }
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
            OrgEntity org) {

        BigDecimal totalTax = taxes.stream()
                .map(FiscalBillTaxEntity::getAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, String> taxNameByLabel = resolveTaxNameByLabel(taxes);

        String html = template;
        html = replace(html, "{{BUSINESS_NAME}}", safe(bill.getEfiscalBusinessname()));
        html = replace(html, "{{BUSINESS_ADDRESS}}", safe(bill.getEfiscalAddress()));
        html = replace(html, "{{BUSINESS_TIN}}", safe(bill.getEfiscalTin()));
        html = replace(html, "{{CUSTOMER_NAME}}", safe(bill.getEfiscalCustomername()));
        html = replace(html, "{{ORDER_ID}}", safe(bill.getOrderId()));
        html = replace(html, "{{INVOICE_TYPE}}", invoiceTypeLabel(bill.getEfiscalInvoicetype()));
        html = replace(html, "{{TRANSACTION_TYPE}}", transactionTypeLabel(bill.getEfiscalTransactiontype()));
        html = replace(html, "{{SDC_INVOICE_NO}}", safe(bill.getEfiscalSdcInvoiceno()));
        html = replace(html, "{{SDC_DATE_TIME}}", formatPfrDateTime(safe(bill.getEfiscalSdcdatetime())));
        html = replace(html, "{{PFR_REQUESTED_BY}}", safe(bill.getEfiscalRequestedby()));
        html = replace(html, "{{TOTAL_AMOUNT}}", formatAmount(bill.getEfiscalTotalamount()));
        html = replace(html, "{{TOTAL_TAX}}", formatAmount(totalTax));
        html = replace(html, "{{EFISCAL_LINK}}", safe(bill.getEfiscalLink()));
        html = replace(html, "{{EFISCAL_QR}}", resolveQrImageSource(bill.getEfiscalQr(), bill.getEfiscalLink()));
        html = replace(html, "{{CASHIER_NAME}}", "");
        html = html.replace("{{ORG_LOGO_BLOCK}}", renderLogoBlock(org));
        html = html.replace("{{LINE_ITEMS_ROWS}}", renderLineRows(lines));
        html = html.replace("{{TAX_ROWS}}", renderTaxRows(taxes, taxNameByLabel));
        html = html.replace("{{PAYMENT_ROWS}}", renderPaymentRows(payments));

        // Advertisement block — rendered as raw HTML (not escaped) only when org has it enabled
        String adBlock = "";
        if (org != null && org.isAdvertisementEnabled()
                && org.getAdvertisementHtml() != null && !org.getAdvertisementHtml().isBlank()) {
            adBlock = org.getAdvertisementHtml();
        }
        html = html.replace("{{ADVERTISEMENT_BLOCK}}", adBlock);

        return html;
    }

    private String renderLineRows(List<FiscalBillLineEntity> lines) {
        if (lines == null || lines.isEmpty()) {
            return "<tr><td colspan=\"5\" class=\"muted\">Nema stavki.</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        BigDecimal lineTotal = BigDecimal.ZERO;
        for (FiscalBillLineEntity line : lines) {
            StringBuilder nameCellContent = new StringBuilder();
            nameCellContent.append(escapeHtml(safe(line.getName())));
            if (line.getGtin() != null && !line.getGtin().isBlank()) {
                nameCellContent.append("<div class=\"item-gtin\">GTIN: ")
                        .append(escapeHtml(line.getGtin()))
                        .append("</div>");
            }
            sb.append("<tr>")
                    .append("<td>").append(nameCellContent).append("</td>")
                    .append("<td>").append(escapeHtml(safe(line.getTaxLabel()))).append("</td>")
                    .append("<td class=\"num\">").append(formatAmount(line.getUnitPrice())).append("</td>")
                    .append("<td class=\"num\">").append(formatAmount(line.getQuantity())).append("</td>")
                    .append("<td class=\"num\">").append(formatAmount(line.getTotalAmount())).append("</td>")
                    .append("</tr>");
            if (line.getTotalAmount() != null) {
                lineTotal = lineTotal.add(line.getTotalAmount());
            }
        }
        sb.append("<tr class=\"line-items-total\">")
                .append("<td colspan=\"4\" class=\"label\">Ukupno:</td>")
                .append("<td class=\"num value\">").append(formatAmount(lineTotal)).append("</td>")
                .append("</tr>");
        return sb.toString();
    }

    private String renderLogoBlock(OrgEntity org) {
        if (org == null || org.getLogoImage() == null || org.getLogoImage().isBlank()) {
            return "";
        }
        String raw = org.getLogoImage().trim();
        String lower = raw.toLowerCase();
        if (!lower.startsWith("data:image/")) {
            return "";
        }
        String src = escapeHtml(raw);
        return "<img class=\"org-logo\" src=\"" + src + "\" alt=\"Organization logo\" />";
    }

    private String renderTaxRows(List<FiscalBillTaxEntity> taxes, Map<String, String> taxNameByLabel) {
        if (taxes == null || taxes.isEmpty()) {
            return "<tr><td colspan=\"4\" class=\"muted\">Nema poreskih stavki.</td></tr>";
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
            return "<tr><td colspan=\"2\" class=\"muted\">Nema placanja.</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        for (FiscalBillPayEntity pay : payments) {
            sb.append("<tr>")
                    .append("<td>").append(escapeHtml(paymentTypeLabel(pay.getPaymentType()))).append("</td>")
                    .append("<td class=\"num\">").append(formatAmount(pay.getAmount())).append("</td>")
                    .append("</tr>");
        }
        return sb.toString();
    }

    private String paymentTypeLabel(Integer paymentType) {
        if (paymentType == null) return "N/A";
        return switch (paymentType) {
            case 0 -> "Other";
            case 1 -> "Cash";
            case 2 -> "Card";
            case 3 -> "Check";
            case 4 -> "WireTransfer";
            case 5 -> "Voucher";
            case 6 -> "MobileMoney";
            default -> "Unknown";
        };
    }

    private String invoiceTypeLabel(Integer invoiceType) {
        if (invoiceType == null) return "N/A";
        return switch (invoiceType) {
            case 0 -> "Promet";
            case 1 -> "Predracun";
            case 2 -> "Kopija";
            case 3 -> "Obuka";
            case 4 -> "Avans";
            default -> "Nepoznato";
        };
    }

    private String transactionTypeLabel(Integer transactionType) {
        if (transactionType == null) return "N/A";
        return transactionType == 1 ? "Refundacija" : "Prodaja";
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

    private String formatPfrDateTime(String value) {
        String input = safe(value).trim();
        if (input.isEmpty()) {
            return "";
        }
        try {
            return OffsetDateTime.parse(input).format(PFR_DISPLAY_FORMAT);
        } catch (DateTimeParseException ignored) {
            // try other common ISO variants
        }
        try {
            return ZonedDateTime.parse(input).format(PFR_DISPLAY_FORMAT);
        } catch (DateTimeParseException ignored) {
            // try local date-time variant
        }
        try {
            return LocalDateTime.parse(input).format(PFR_DISPLAY_FORMAT);
        } catch (DateTimeParseException ignored) {
            // try instant variant
        }
        try {
            return Instant.parse(input).atOffset(java.time.ZoneOffset.UTC).format(PFR_DISPLAY_FORMAT);
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

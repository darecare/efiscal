package com.efiscal.backend.service;

import com.efiscal.backend.model.FiscalBillEntity;
import com.efiscal.backend.model.FiscalBillLineEntity;
import com.efiscal.backend.model.FiscalBillPayEntity;
import com.efiscal.backend.model.FiscalBillTaxEntity;
import com.efiscal.backend.repository.FiscalBillLineRepository;
import com.efiscal.backend.repository.FiscalBillPayRepository;
import com.efiscal.backend.repository.FiscalBillRepository;
import com.efiscal.backend.repository.FiscalBillTaxRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FiscalBillPdfService {

    public enum PdfTemplateFormat {
        A4,
        ROLL80
    }

    private final FiscalBillRepository fiscalBillRepository;
    private final FiscalBillLineRepository fiscalBillLineRepository;
    private final FiscalBillTaxRepository fiscalBillTaxRepository;
    private final FiscalBillPayRepository fiscalBillPayRepository;

    public FiscalBillPdfService(
            FiscalBillRepository fiscalBillRepository,
            FiscalBillLineRepository fiscalBillLineRepository,
            FiscalBillTaxRepository fiscalBillTaxRepository,
            FiscalBillPayRepository fiscalBillPayRepository) {
        this.fiscalBillRepository = fiscalBillRepository;
        this.fiscalBillLineRepository = fiscalBillLineRepository;
        this.fiscalBillTaxRepository = fiscalBillTaxRepository;
        this.fiscalBillPayRepository = fiscalBillPayRepository;
    }

    public byte[] generateDefaultA4Pdf(Long fiscalBillId) {
        return generatePdf(fiscalBillId, PdfTemplateFormat.A4);
    }

    public byte[] generatePdf(Long fiscalBillId, PdfTemplateFormat format) {
        FiscalBillEntity bill = fiscalBillRepository.findById(fiscalBillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fiscal bill not found"));

        List<FiscalBillLineEntity> lines = fiscalBillLineRepository.findByFiscalbillId(fiscalBillId);
        List<FiscalBillTaxEntity> taxes = fiscalBillTaxRepository.findByFiscalbillId(fiscalBillId);
        List<FiscalBillPayEntity> payments = fiscalBillPayRepository.findByFiscalbillId(fiscalBillId);

        String template = readTemplate(resolveTemplatePath(format));
        String html = renderTemplate(template, bill, lines, taxes, payments);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
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
            List<FiscalBillPayEntity> payments) {

        BigDecimal totalTax = taxes.stream()
                .map(FiscalBillTaxEntity::getAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String html = template;
        html = replace(html, "{{BUSINESS_NAME}}", safe(bill.getEfiscalBusinessname()));
        html = replace(html, "{{BUSINESS_ADDRESS}}", safe(bill.getEfiscalAddress()));
        html = replace(html, "{{BUSINESS_TIN}}", safe(bill.getEfiscalTin()));
        html = replace(html, "{{CUSTOMER_NAME}}", safe(bill.getEfiscalCustomername()));
        html = replace(html, "{{ORDER_ID}}", safe(bill.getOrderId()));
        html = replace(html, "{{INVOICE_TYPE}}", invoiceTypeLabel(bill.getEfiscalInvoicetype()));
        html = replace(html, "{{TRANSACTION_TYPE}}", transactionTypeLabel(bill.getEfiscalTransactiontype()));
        html = replace(html, "{{SDC_INVOICE_NO}}", safe(bill.getEfiscalSdcInvoiceno()));
        html = replace(html, "{{SDC_DATE_TIME}}", safe(bill.getEfiscalSdcdatetime()));
        html = replace(html, "{{PFR_REQUESTED_BY}}", safe(bill.getEfiscalRequestedby()));
        html = replace(html, "{{TOTAL_AMOUNT}}", formatAmount(bill.getEfiscalTotalamount()));
        html = replace(html, "{{TOTAL_TAX}}", formatAmount(totalTax));
        html = replace(html, "{{EFISCAL_LINK}}", safe(bill.getEfiscalLink()));
        html = html.replace("{{LINE_ITEMS_ROWS}}", renderLineRows(lines));
        html = html.replace("{{TAX_ROWS}}", renderTaxRows(taxes));
        html = html.replace("{{PAYMENT_ROWS}}", renderPaymentRows(payments));
        return html;
    }

    private String renderLineRows(List<FiscalBillLineEntity> lines) {
        if (lines == null || lines.isEmpty()) {
            return "<tr><td colspan=\"5\" class=\"muted\">Nema stavki.</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        for (FiscalBillLineEntity line : lines) {
            sb.append("<tr>")
                    .append("<td>").append(escapeHtml(safe(line.getName()))).append("</td>")
                    .append("<td>").append(escapeHtml(safe(line.getTaxLabel()))).append("</td>")
                    .append("<td class=\"num\">").append(formatAmount(line.getUnitPrice())).append("</td>")
                    .append("<td class=\"num\">").append(formatAmount(line.getQuantity())).append("</td>")
                    .append("<td class=\"num\">").append(formatAmount(line.getTotalAmount())).append("</td>")
                    .append("</tr>");
            if (line.getGtin() != null && !line.getGtin().isBlank()) {
                sb.append("<tr>")
                        .append("<td colspan=\"5\" class=\"subline\">GTIN: ")
                        .append(escapeHtml(line.getGtin()))
                        .append("</td>")
                        .append("</tr>");
            }
        }
        return sb.toString();
    }

    private String renderTaxRows(List<FiscalBillTaxEntity> taxes) {
        if (taxes == null || taxes.isEmpty()) {
            return "<tr><td colspan=\"4\" class=\"muted\">Nema poreskih stavki.</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        for (FiscalBillTaxEntity tax : taxes) {
            sb.append("<tr>")
                    .append("<td>").append(escapeHtml(safe(tax.getEfiscalTaxlabel()))).append("</td>")
                    .append("<td>").append(escapeHtml(safe(tax.getEfiscalCategoryname()))).append("</td>")
                    .append("<td class=\"num\">").append(formatPercent(tax.getRate())).append("</td>")
                    .append("<td class=\"num\">").append(formatAmount(tax.getAmount())).append("</td>")
                    .append("</tr>");
        }
        return sb.toString();
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

    private String formatAmount(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) {
            return "0.00%";
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + "%";
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

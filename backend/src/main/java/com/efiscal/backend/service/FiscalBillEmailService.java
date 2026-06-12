package com.efiscal.backend.service;

import com.efiscal.backend.model.EmailLogEntity;
import com.efiscal.backend.model.EmailTemplateEntity;
import com.efiscal.backend.model.FiscalBillEntity;
import com.efiscal.backend.model.OrgEntity;
import com.efiscal.backend.repository.EmailLogRepository;
import com.efiscal.backend.repository.EmailTemplateRepository;
import com.efiscal.backend.repository.OrgRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.core.io.ByteArrayResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import jakarta.mail.internet.MimeMessage;

@Service
public class FiscalBillEmailService {

    private static final Logger log = LoggerFactory.getLogger(FiscalBillEmailService.class);
    private static final int DEFAULT_SMTP_PORT = 25;

    private final OrgRepository orgRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailLogRepository emailLogRepository;
    private final FiscalBillPdfService fiscalBillPdfService;

    public FiscalBillEmailService(
            OrgRepository orgRepository,
            EmailTemplateRepository emailTemplateRepository,
            EmailLogRepository emailLogRepository,
            FiscalBillPdfService fiscalBillPdfService) {
        this.orgRepository = orgRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.emailLogRepository = emailLogRepository;
        this.fiscalBillPdfService = fiscalBillPdfService;
    }

    public void sendIfRequested(Long orgId, FiscalBillEntity bill, boolean sendEmail, String customerEmail,
            String customerName, String orderId) {
        if (!sendEmail) {
            return;
        }

        String recipient = customerEmail == null ? "" : customerEmail.trim();
        if (recipient.isBlank()) {
            logEmail(orgId, bill, orderId, recipient, null, null, null, "SKIPPED", "Customer email is missing");
            return;
        }

        try {
            OrgEntity org = orgRepository.findById(orgId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
            EmailTemplateEntity template = emailTemplateRepository
                    .findTopByOrgOrgIdAndDeletedAtIsNullAndIsActiveTrueOrderByUpdatedAtDesc(orgId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active email template found for organization"));

            Map<String, String> values = buildTemplateValues(bill, orderId, customerName);
            String subject = applyTemplate(template.getSubject(), values);
            String body = applyTemplate(template.getBodyHtml(), values);
            byte[] pdfBytes = fiscalBillPdfService.generateDefaultA4Pdf(bill.getFiscalbillId());
            String pdfFilename = buildAttachmentFilename(bill);

            JavaMailSender mailSender = buildMailSender(org);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipient);
            helper.setFrom(resolveFromAddress(org));
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.addAttachment(pdfFilename, new ByteArrayResource(pdfBytes), "application/pdf");
            mailSender.send(message);

            logEmail(orgId, bill, orderId, recipient, template.getTemplateName(), subject, body, "SENT", null);
        } catch (Exception ex) {
            log.warn("Failed to send fiscal bill email for bill {}: {}", bill.getFiscalbillId(), ex.getMessage());
            logEmail(orgId, bill, orderId, recipient, null, null, null, "FAILED", ex.getMessage());
        }
    }

    private JavaMailSender buildMailSender(OrgEntity org) {
        if (org.getSmtpServer() == null || org.getSmtpServer().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMTP server is not configured for organization");
        }

        int smtpPort = org.getSmtpPort() != null ? org.getSmtpPort() : DEFAULT_SMTP_PORT;
        validateSmtpPort(smtpPort);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(org.getSmtpServer());
        sender.setPort(smtpPort);
        if (org.getSmtpUsername() != null && !org.getSmtpUsername().isBlank()) {
            sender.setUsername(org.getSmtpUsername());
        }
        if (org.getSmtpPassword() != null && !org.getSmtpPassword().isBlank()) {
            sender.setPassword(org.getSmtpPassword());
        }

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(org.getSmtpUsername() != null && !org.getSmtpUsername().isBlank()));
        props.put("mail.smtp.starttls.enable", String.valueOf(isStartTlsEnabled(org.getSmtpConnectionSecurity())));
        props.put("mail.smtp.ssl.enable", String.valueOf(isSslTlsEnabled(org.getSmtpConnectionSecurity())));
        props.put("mail.smtp.ssl.trust", org.getSmtpServer());
        return sender;
    }

    private void validateSmtpPort(int smtpPort) {
        if (smtpPort == 143 || smtpPort == 993) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SMTP port is configured as IMAP port (" + smtpPort + "). Use SMTP port 587 (STARTTLS) or 465 (SSL_TLS)."
            );
        }
        if (smtpPort == 110 || smtpPort == 995) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SMTP port is configured as POP3 port (" + smtpPort + "). Use SMTP port 587 (STARTTLS) or 465 (SSL_TLS)."
            );
        }
    }

    private boolean isStartTlsEnabled(String connectionSecurity) {
        return "STARTTLS".equalsIgnoreCase(connectionSecurity);
    }

    private boolean isSslTlsEnabled(String connectionSecurity) {
        return "SSL_TLS".equalsIgnoreCase(connectionSecurity) || "SSL/TLS".equalsIgnoreCase(connectionSecurity);
    }

    private String buildAttachmentFilename(FiscalBillEntity bill) {
        String invoiceNo = safe(bill.getEfiscalSdcInvoiceno()).trim();
        if (!invoiceNo.isEmpty()) {
            return "fiscal-bill-" + sanitizeFilenamePart(invoiceNo) + ".pdf";
        }
        return "fiscal-bill-" + bill.getFiscalbillId() + ".pdf";
    }

    private String sanitizeFilenamePart(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String resolveFromAddress(OrgEntity org) {
        if (org.getEmailFrom() != null && !org.getEmailFrom().isBlank()) {
            return org.getEmailFrom();
        }
        if (org.getSmtpUsername() != null && !org.getSmtpUsername().isBlank()) {
            return org.getSmtpUsername();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "From email is not configured for organization");
    }

    private Map<String, String> buildTemplateValues(FiscalBillEntity bill, String orderId, String customerName) {
        Map<String, String> values = new HashMap<>();
        values.put("customername", safe(customerName != null ? customerName : bill.getEfiscalCustomername()));
        values.put("customer_name", safe(customerName != null ? customerName : bill.getEfiscalCustomername()));
        values.put("order_id", safe(orderId != null ? orderId : bill.getOrderId()));
        values.put("invoice_number", safe(bill.getEfiscalSdcInvoiceno()));
        values.put("invoicetype", bill.getEfiscalInvoicetype() == null ? "" : String.valueOf(bill.getEfiscalInvoicetype()));
        values.put("transactiontype", bill.getEfiscalTransactiontype() == null ? "" : String.valueOf(bill.getEfiscalTransactiontype()));
        values.put("fiscal_link", safe(bill.getEfiscalLink()));
        values.put("total_amount", bill.getEfiscalTotalamount() == null ? "" : bill.getEfiscalTotalamount().toPlainString());
        return values;
    }

    private String applyTemplate(String template, Map<String, String> values) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String replacement = htmlEscape(entry.getValue());
            rendered = rendered
                    .replace("{{ " + entry.getKey() + " }}", replacement)
                    .replace("{{" + entry.getKey() + "}}", replacement)
                    .replace("{{" + entry.getKey() + " }}", replacement)
                    .replace("{{ " + entry.getKey() + "}}", replacement);
        }
        return rendered;
    }

    private void logEmail(Long orgId, FiscalBillEntity bill, String orderId, String recipientEmail,
            String templateName, String subject, String bodyHtml, String status, String errorMessage) {
        EmailLogEntity logEntry = new EmailLogEntity();
        logEntry.setOrgId(orgId);
        logEntry.setFiscalbillId(bill.getFiscalbillId());
        logEntry.setOrderId(orderId != null ? orderId : bill.getOrderId());
        logEntry.setRecipientEmail(recipientEmail);
        logEntry.setTemplateName(templateName);
        logEntry.setSubject(subject);
        logEntry.setBodyHtml(bodyHtml);
        logEntry.setStatus(status);
        logEntry.setErrorMessage(errorMessage);
        if ("SENT".equals(status)) {
            logEntry.setSentAt(OffsetDateTime.now());
        }
        emailLogRepository.save(logEntry);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String htmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

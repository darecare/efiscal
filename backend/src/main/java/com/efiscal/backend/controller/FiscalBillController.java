package com.efiscal.backend.controller;

import com.efiscal.backend.model.AppUserEntity;
import com.efiscal.backend.repository.AppUserRepository;
import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.FiscalBillService;
import com.efiscal.backend.service.FiscalBillService.FiscalBillItemRequest;
import com.efiscal.backend.service.FiscalBillService.ManualFiscalBillRequest;
import com.efiscal.backend.service.FiscalBillService.OrderFiscalizeRequest;
import com.efiscal.backend.service.FiscalBillService.PaymentRequest;
import com.efiscal.backend.service.FiscalBillPdfService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscalbill")
public class FiscalBillController {

    private final FiscalBillService fiscalBillService;
    private final FiscalBillPdfService fiscalBillPdfService;
    private final AuthorizationService authorizationService;
    private final com.efiscal.backend.repository.FiscalBillRepository fiscalBillRepository;
    private final AppUserRepository appUserRepository;

    public FiscalBillController(
            FiscalBillService fiscalBillService,
            FiscalBillPdfService fiscalBillPdfService,
            AuthorizationService authorizationService,
            com.efiscal.backend.repository.FiscalBillRepository fiscalBillRepository,
            AppUserRepository appUserRepository) {
        this.fiscalBillService = fiscalBillService;
        this.fiscalBillPdfService = fiscalBillPdfService;
        this.authorizationService = authorizationService;
        this.fiscalBillRepository = fiscalBillRepository;
        this.appUserRepository = appUserRepository;
    }

    private String resolveCurrentUserCashier() {
        String userIdStr = authorizationService.getUserId();
        if (userIdStr == null) return null;
        try {
            return appUserRepository.findById(Long.parseLong(userIdStr))
                    .map(AppUserEntity::getCashier)
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void validateOrg(Long orgId) {
        authorizationService.requireOrgAccess(orgId);
    }

    private void validateFiscalBill(Long fiscalBillId) {
        if (fiscalBillId == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "id is required");
        }
        if (authorizationService.isSuperAdmin()) {
            return;
        }
        com.efiscal.backend.model.FiscalBillEntity bill = fiscalBillRepository.findById(fiscalBillId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Fiscal bill not found"));
        authorizationService.requireOrgAccess(bill.getOrgId());
    }

    /** GET /api/v1/fiscalbill/status?orgId=N — Tax Authority status */
    @GetMapping("/status")
    public ResponseEntity<?> getTaxAuthorityStatus(@RequestParam(required = false) Long orgId) {
        authorizationService.requireAction("FISCAL_VIEW_BILLS");
        if (orgId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("orgId query parameter is required"));
        }
        validateOrg(orgId);
        Map<String, Object> statusResponse = fiscalBillService.getStatus(orgId);
        return ResponseEntity.ok(statusResponse);
    }

    /** GET /api/v1/fiscalbill?orgId=N — List fiscal bills for organization */
    @GetMapping
    public ResponseEntity<?> listFiscalBills(@RequestParam(required = false) Long orgId) {
        authorizationService.requireAction("FISCAL_VIEW_BILLS");
        if (orgId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("orgId query parameter is required"));
        }
        validateOrg(orgId);
        return ResponseEntity.ok(fiscalBillService.listFiscalBills(orgId));
    }

    /** GET /api/v1/fiscalbill/{id} — Retrieve fiscal bill */
    @GetMapping("/{id}")
    public ResponseEntity<?> getFiscalBill(@PathVariable Long id) {
        authorizationService.requireAction("FISCAL_VIEW_BILLS");
        validateFiscalBill(id);
        FiscalBillService.FiscalBillView fiscalBill = fiscalBillService.findFiscalBillById(id);
        if (fiscalBill == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Fiscal bill not found"));
        }
        return ResponseEntity.ok(fiscalBill);
    }

    /** GET /api/v1/fiscalbill/{id}/details — Retrieve fiscal bill related tax/payment rows */
    @GetMapping("/{id}/details")
    public ResponseEntity<?> getFiscalBillDetails(@PathVariable Long id) {
        authorizationService.requireAction("FISCAL_VIEW_BILLS");
        validateFiscalBill(id);
        FiscalBillService.FiscalBillDetailsView details = fiscalBillService.findFiscalBillDetails(id);
        if (details == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Fiscal bill not found"));
        }
        return ResponseEntity.ok(details);
    }

    /** GET /api/v1/fiscalbill/{id}/pdf — Download fiscal bill PDF rendered from selected HTML template */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadFiscalBillPdf(
            @PathVariable Long id,
            @RequestParam(name = "format", required = false, defaultValue = "a4") String format) {
        authorizationService.requireAction("FISCAL_VIEW_BILLS");
        validateFiscalBill(id);
        FiscalBillPdfService.PdfTemplateFormat templateFormat = fiscalBillPdfService.parseTemplateFormat(format);
        byte[] pdf = fiscalBillPdfService.generatePdf(id, templateFormat);
        String suffix = fiscalBillPdfService.filenameSuffix(templateFormat);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fiscal-bill-" + id + "-" + suffix + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * POST /api/v1/fiscalbill/from-order — 4.1 Order-Based Fiscalization
     * Creates a fiscal bill from a sales order with all 4.1.x rules applied.
     */
    @PostMapping("/from-order")
    public ResponseEntity<?> createFromOrder(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long clientId,
            @RequestBody CreateFromOrderRequest request) {
        authorizationService.requireAction("FISCAL_CREATE_BILL");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Idempotency-Key header is required"));
        }
        if (orgId == null || clientId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("orgId and clientId query parameters are required"));
        }
        validateOrg(orgId);
        if (!authorizationService.isSuperAdmin() && !clientId.equals(authorizationService.getClientId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Access denied"));
        }

        List<FiscalBillItemRequest> items = request.items() == null ? List.of() :
                request.items().stream().map(i -> new FiscalBillItemRequest(
                        i.name(), i.quantity(), i.unitPrice(), i.totalAmount(),
                i.taxLabel(), i.taxPrefix(), i.gtin(), i.productId(), i.sku(), i.taxValue(), i.taxCategoryName(), i.labels()
                )).toList();

        OrderFiscalizeRequest orderData = new OrderFiscalizeRequest(
                request.orderId(), request.customerName(),
            request.customerEmail(), request.sendEmail(),
                request.billingType(), request.billingCompanyVat(),
                request.paymentMethodCode(), items,
                resolveCurrentUserCashier());

        FiscalBillService.FiscalBillCreateResult result = fiscalBillService.createFiscalBillFromOrder(
                orgId, clientId, idempotencyKey,
                request.orderId(), request.invoiceType(), request.transactionType(), orderData);

        if (result.alreadyExists()) {
            return ResponseEntity.ok(result.fiscalBill());
        }
        if (result.failed()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result.fiscalBill());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.fiscalBill());
    }

    /**
     * POST /api/v1/fiscalbill/manual — 4.2 Manual Fiscal Bill Creation
     * Applies same business rules as 4.1.
     */
    @PostMapping("/manual")
    public ResponseEntity<?> createManual(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long clientId,
            @RequestBody CreateManualRequest request) {
        authorizationService.requireAction("FISCAL_CREATE_BILL");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Idempotency-Key header is required"));
        }
        if (orgId == null || clientId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("orgId and clientId query parameters are required"));
        }
        validateOrg(orgId);
        if (!authorizationService.isSuperAdmin() && !clientId.equals(authorizationService.getClientId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Access denied"));
        }

        List<FiscalBillItemRequest> items = request.items() == null ? List.of() :
                request.items().stream().map(i -> new FiscalBillItemRequest(
                        i.name(), i.quantity(), i.unitPrice(), i.totalAmount(),
                i.taxLabel(), i.taxPrefix(), i.gtin(), i.productId(), i.sku(), i.taxValue(), i.taxCategoryName(), i.labels()
                )).toList();

        List<PaymentRequest> payments = request.payments() == null ? List.of() :
                request.payments().stream().map(p -> new PaymentRequest(p.paymentType(), p.amount())).toList();

        ManualFiscalBillRequest manualRequest = new ManualFiscalBillRequest(
                request.orderId(), request.customerName(),
            request.customerEmail(), request.sendEmail(),
                request.invoiceType(), request.transactionType(),
                request.buyerType(), request.buyerVat(),
                items, payments,
                request.referentDocumentNumber(),
                resolveCurrentUserCashier());

        FiscalBillService.FiscalBillCreateResult result = fiscalBillService.createManualFiscalBill(
                orgId, clientId, idempotencyKey, manualRequest);

        if (result.alreadyExists()) {
            return ResponseEntity.ok(result.fiscalBill());
        }
        if (result.failed()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result.fiscalBill());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.fiscalBill());
    }

    /** POST /api/v1/fiscalbill/{id}/retry — Retry failed fiscal bill */
    @PostMapping("/{id}/retry")
    public ResponseEntity<?> retryFiscalBill(
            @PathVariable Long id,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        authorizationService.requireAction("FISCAL_RETRY_BILL");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Idempotency-Key header is required"));
        }
        validateFiscalBill(id);
        FiscalBillService.FiscalBillRetryResult result = fiscalBillService.retryFiscalBill(id, idempotencyKey);
        if (result.notFound()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Fiscal bill not found"));
        }
        if (result.idempotencyConflict()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Idempotency-Key already used for another fiscal bill"));
        }
        if (result.notRetryable()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Only FAILED fiscal bills can be retried"));
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result.fiscalBill());
    }

    /** POST /api/v1/fiscalbill/{id}/copy — Create Copy fiscal bill (invoiceType=2) */
    @PostMapping("/{id}/copy")
    public ResponseEntity<?> createCopyFiscalBill(
            @PathVariable Long id,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        authorizationService.requireAction("FISCAL_CREATE_BILL");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Idempotency-Key header is required"));
        }
        validateFiscalBill(id);
        FiscalBillService.FiscalBillCreateResult result = fiscalBillService.createCopyFiscalBill(id, idempotencyKey);
        if (result.alreadyExists()) {
            return ResponseEntity.ok(result.fiscalBill());
        }
        if (result.failed()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result.fiscalBill());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.fiscalBill());
    }

    /** POST /api/v1/fiscalbill/{id}/refund — Create Refund fiscal bill from source Sale bill */
    @PostMapping("/{id}/refund")
    public ResponseEntity<?> createRefundFiscalBill(
            @PathVariable Long id,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        authorizationService.requireAction("FISCAL_CREATE_BILL");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Idempotency-Key header is required"));
        }
        validateFiscalBill(id);
        FiscalBillService.FiscalBillCreateResult result = fiscalBillService.createRefundFiscalBill(id, idempotencyKey);
        if (result.alreadyExists()) {
            return ResponseEntity.ok(result.fiscalBill());
        }
        if (result.failed()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result.fiscalBill());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.fiscalBill());
    }

    // -----------------------------------------------------------------------
    // Request records
    // -----------------------------------------------------------------------

    public record ItemRequest(
            String name,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            String taxLabel,
            String taxPrefix,
            String gtin,
            String productId,
            String sku,
            BigDecimal taxValue,
            String taxCategoryName,
            List<String> labels) {}

    public record PaymentRowRequest(int paymentType, BigDecimal amount) {}

    public record CreateFromOrderRequest(
            String orderId,
            String customerName,
            String customerEmail,
            boolean sendEmail,
            int invoiceType,
            int transactionType,
            String billingType,
            String billingCompanyVat,
            String paymentMethodCode,
            List<ItemRequest> items) {}

    public record CreateManualRequest(
            String orderId,        // optional — links to existing order
            String customerName,
            String customerEmail,
            boolean sendEmail,
            int invoiceType,
            int transactionType,
            String buyerType,      // optional buyer type prefix
            String buyerVat,       // optional company VAT
            List<ItemRequest> items,
            List<PaymentRowRequest> payments,
            String referentDocumentNumber) {}

    public record ErrorResponse(String message) {}
}


package com.efiscal.backend.controller;

import com.efiscal.backend.service.FiscalBillService;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
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

    public FiscalBillController(FiscalBillService fiscalBillService) {
        this.fiscalBillService = fiscalBillService;
    }

    @PostMapping
    public ResponseEntity<?> createFiscalBill(
        @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
        @RequestBody CreateFiscalBillRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Idempotency-Key header is required"));
        }
        FiscalBillService.FiscalBillCreateRequest payload = new FiscalBillService.FiscalBillCreateRequest(
            request.OrderId(),
            new FiscalBillService.CustomerPayload(request.customer() != null ? request.customer().name() : null),
            request.items() == null
                ? List.of()
                : request.items().stream()
                    .map(item -> new FiscalBillService.FiscalItemPayload(item.sku(), item.name(), item.quantity(), item.unitPrice(), item.taxRate()))
                    .toList(),
            request.currency(),
            request.paymentMethod());

        FiscalBillService.FiscalBillCreateResult result = fiscalBillService.createFiscalBill(idempotencyKey, payload);
        FiscalBillCreateResponse body = new FiscalBillCreateResponse(
            result.fiscalBill().fiscalDocumentId(),
            result.fiscalBill().status(),
            result.fiscalBill().createdAt());

        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/status")
    public ResponseEntity<?> getTaxAuthorityStatus(@RequestParam(required = false) Long orgId) {
        if (orgId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("orgId query parameter is required"));
        }
        Map<String, Object> statusResponse = fiscalBillService.getStatus(orgId);
        return ResponseEntity.ok(statusResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFiscalBill(@PathVariable String id) {
        FiscalBillService.FiscalBillView fiscalBill = fiscalBillService.findFiscalBillById(id);
        if (fiscalBill == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Fiscal bill not found"));
        }
        FiscalBillStatusResponse response = new FiscalBillStatusResponse(
            fiscalBill.fiscalDocumentId(),
            fiscalBill.status(),
            fiscalBill.providerReference(),
            fiscalBill.lastError(),
            fiscalBill.attemptCount(),
            fiscalBill.updatedAt());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<?> retryFiscalBill(
        @PathVariable String id,
        @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Idempotency-Key header is required"));
        }

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

        FiscalBillRetryResponse response = new FiscalBillRetryResponse(
            result.fiscalBill().fiscalDocumentId(),
            result.fiscalBill().status());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    public record CreateFiscalBillRequest(
        @NotBlank String OrderId,
        CustomerRequest customer,
        List<FiscalItemRequest> items,
        @NotBlank String currency,
        @NotBlank String paymentMethod) {
    }

    public record CustomerRequest(String name) {
    }

    public record FiscalItemRequest(
        String sku,
        String name,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate) {
    }

    public record FiscalBillCreateResponse(String fiscalDocumentId, String status, String createdAt) {
    }

    public record FiscalBillStatusResponse(
        String fiscalDocumentId,
        String status,
        String providerReference,
        String lastError,
        int attemptCount,
        String updatedAt) {
    }

    public record FiscalBillRetryResponse(String fiscalDocumentId, String status) {
    }

    public record ErrorResponse(String message) {
    }
}

package com.efiscal.backend.service;

import com.efiscal.backend.repository.FiscalBillConfigRepository;
import com.efiscal.backend.repository.FiscalBillIdempotencyKeyRepository;
import com.efiscal.backend.repository.FiscalBillLineRepository;
import com.efiscal.backend.repository.FiscalBillPayRepository;
import com.efiscal.backend.repository.FiscalBillRepository;
import com.efiscal.backend.repository.FiscalBillTaxRepository;
import com.efiscal.backend.repository.PayTypeMapRepository;
import com.efiscal.backend.repository.TaxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FiscalBillServiceManualTest {

    private static final Long ORG_ID = 1L;
    private static final Long CLIENT_ID = 10L;

    @Mock private FiscalBillRepository fiscalBillRepository;
    @Mock private FiscalBillTaxRepository fiscalBillTaxRepository;
    @Mock private FiscalBillPayRepository fiscalBillPayRepository;
    @Mock private FiscalBillLineRepository fiscalBillLineRepository;
    @Mock private FiscalBillIdempotencyKeyRepository idempotencyKeyRepository;
    @Mock private FiscalBillConfigRepository fiscalBillConfigRepository;
    @Mock private PayTypeMapRepository payTypeMapRepository;
    @Mock private TaxRepository taxRepository;
    @Mock private TaxAuthorityService taxAuthorityService;
    @Mock private FiscalBillEmailService fiscalBillEmailService;

    private FiscalBillService fiscalBillService;

    @BeforeEach
    void setUp() {
        fiscalBillService = new FiscalBillService(
                fiscalBillRepository,
                fiscalBillTaxRepository,
                fiscalBillPayRepository,
                fiscalBillLineRepository,
                idempotencyKeyRepository,
                fiscalBillConfigRepository,
                payTypeMapRepository,
                taxRepository,
                taxAuthorityService,
                fiscalBillEmailService,
                new ObjectMapper()
        );
        when(idempotencyKeyRepository.findById(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void createManualFiscalBill_rejectsPaymentTotalMismatch() {
        FiscalBillService.ManualFiscalBillRequest request = manualRequest(
                null,
                List.of(item("Product", "100.00")),
                List.of(payment(1, "50.00"))
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-1", request));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Payment total does not match fiscal bill total", ex.getReason());
        verify(fiscalBillRepository, never()).save(any());
    }

    @Test
    void createManualFiscalBill_rejectsUnknownReferentDocument() {
        when(fiscalBillRepository.findFirstByOrgIdAndEfiscalSdcInvoicenoOrderByCreatedDesc(
                ORG_ID, "REF-UNKNOWN")).thenReturn(Optional.empty());

        FiscalBillService.ManualFiscalBillRequest request = new FiscalBillService.ManualFiscalBillRequest(
                null,
                null,
                null,
                false,
                FiscalBillService.INVOICE_TYPE_COPY,
                FiscalBillService.TRANSACTION_TYPE_SALE,
                null,
                null,
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00")),
                "REF-UNKNOWN",
                null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-2", request));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Referenced fiscal bill not found for number: REF-UNKNOWN", ex.getReason());
        verify(fiscalBillRepository, never()).save(any());
    }

    @Test
    void createManualFiscalBill_rejectsDuplicateOrderBill() {
        var existing = new com.efiscal.backend.model.FiscalBillEntity();
        existing.setStatus(FiscalBillService.STATUS_SUCCESS);
        when(fiscalBillRepository.findLatestByOrgAndOrderAndType(
                ORG_ID, "ORD-1", FiscalBillService.INVOICE_TYPE_NORMAL, FiscalBillService.TRANSACTION_TYPE_SALE))
                .thenReturn(Optional.of(existing));

        FiscalBillService.ManualFiscalBillRequest request = manualRequest(
                "ORD-1",
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00"))
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-3", request));

        assertEquals(409, ex.getStatusCode().value());
        verify(fiscalBillRepository, never()).save(any());
    }

    private static FiscalBillService.ManualFiscalBillRequest manualRequest(
            String orderId,
            List<FiscalBillService.FiscalBillItemRequest> items,
            List<FiscalBillService.PaymentRequest> payments) {
        return new FiscalBillService.ManualFiscalBillRequest(
                orderId,
                null,
                null,
                false,
                FiscalBillService.INVOICE_TYPE_NORMAL,
                FiscalBillService.TRANSACTION_TYPE_SALE,
                null,
                null,
                items,
                payments,
                null,
                null
        );
    }

    private static FiscalBillService.FiscalBillItemRequest item(String name, String total) {
        return new FiscalBillService.FiscalBillItemRequest(
                name,
                BigDecimal.ONE,
                new BigDecimal(total),
                new BigDecimal(total),
                "A",
                "20",
                null,
                null,
                null,
                null,
                null,
                List.of("A")
        );
    }

    private static FiscalBillService.PaymentRequest payment(int type, String amount) {
        return new FiscalBillService.PaymentRequest(type, new BigDecimal(amount));
    }
}

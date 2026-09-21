package com.efiscal.backend.service;

import com.efiscal.backend.model.FiscalBillEntity;
import com.efiscal.backend.model.FiscalBillLineEntity;
import com.efiscal.backend.model.FiscalBillPayEntity;
import com.efiscal.backend.model.TaxEntity;
import com.efiscal.backend.repository.FiscalBillIdempotencyKeyRepository;
import com.efiscal.backend.repository.FiscalBillLineRepository;
import com.efiscal.backend.repository.FiscalBillPayRepository;
import com.efiscal.backend.repository.FiscalBillRepository;
import com.efiscal.backend.repository.FiscalBillTaxRepository;
import com.efiscal.backend.repository.PayTypeMapRepository;
import com.efiscal.backend.repository.ProductRepository;
import com.efiscal.backend.repository.TaxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FiscalBillServiceManualTest {

    private static final Long ORG_ID = 1L;
    private static final Long CLIENT_ID = 10L;
    private static final ZoneId BELGRADE_ZONE = ZoneId.of("Europe/Belgrade");
    private static final String TA_SUCCESS_RESPONSE = """
            {
              "invoiceNumber": "INV-TEST-1",
              "sdcDateTime": "2024-06-01T10:00:00+02:00",
              "verificationUrl": "https://example.com/verify",
              "verificationQRCode": "data:image/png;base64,abc",
              "totalAmount": 100.00
            }
            """;

    @Mock private FiscalBillRepository fiscalBillRepository;
    @Mock private FiscalBillTaxRepository fiscalBillTaxRepository;
    @Mock private FiscalBillPayRepository fiscalBillPayRepository;
    @Mock private FiscalBillLineRepository fiscalBillLineRepository;
    @Mock private FiscalBillIdempotencyKeyRepository idempotencyKeyRepository;
    @Mock private PayTypeMapRepository payTypeMapRepository;
        @Mock private ProductRepository productRepository;
    @Mock private TaxRepository taxRepository;
    @Mock private TaxAuthorityService taxAuthorityService;
    @Mock private FiscalBillEmailService fiscalBillEmailService;

    private FiscalBillService fiscalBillService;
    private final AtomicLong nextBillId = new AtomicLong(100L);

    @BeforeEach
    void setUp() {
        fiscalBillService = new FiscalBillService(
                fiscalBillRepository,
                fiscalBillTaxRepository,
                fiscalBillPayRepository,
                fiscalBillLineRepository,
                idempotencyKeyRepository,
                payTypeMapRepository,
                productRepository,
                taxRepository,
                taxAuthorityService,
                fiscalBillEmailService,
                new EsirNumberService("123456", "1.0.0"),
                new ObjectMapper()
        );
        when(idempotencyKeyRepository.findById(anyString())).thenReturn(Optional.empty());
        when(fiscalBillRepository.save(any(FiscalBillEntity.class))).thenAnswer(invocation -> {
            FiscalBillEntity entity = invocation.getArgument(0);
            if (entity.getFiscalbillId() == null) {
                entity.setFiscalbillId(nextBillId.getAndIncrement());
            }
            return entity;
        });
        when(fiscalBillEmailService.sendIfRequested(anyLong(), any(FiscalBillEntity.class), any(Boolean.class), any(), any(), any()))
                .thenReturn(FiscalBillEmailService.EmailSendResult.sent());
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

        FiscalBillService.ManualFiscalBillRequest request = copyRequest(
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00")),
                "REF-UNKNOWN"
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-2", request));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Referenced fiscal bill not found for number: REF-UNKNOWN", ex.getReason());
        verify(fiscalBillRepository, never()).save(any());
    }

    @Test
    void createManualFiscalBill_rejectsReferentDocumentMissingDatetime() {
        FiscalBillEntity ref = new FiscalBillEntity();
        ref.setEfiscalSdcInvoiceno("REF-NO-DT");
        when(fiscalBillRepository.findFirstByOrgIdAndEfiscalSdcInvoicenoOrderByCreatedDesc(
                ORG_ID, "REF-NO-DT")).thenReturn(Optional.of(ref));

        FiscalBillService.ManualFiscalBillRequest request = copyRequest(
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00")),
                "REF-NO-DT"
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-ref-dt", request));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("missing Tax Authority datetime"));
        verify(fiscalBillRepository, never()).save(any());
    }

    @Test
    void createManualFiscalBill_resolvesReferentDocumentDatetime() throws Exception {
        FiscalBillEntity ref = new FiscalBillEntity();
        ref.setEfiscalSdcInvoiceno("REF-OK");
        ref.setEfiscalSdcdatetime("2024-05-01T12:00:00+02:00");
        when(fiscalBillRepository.findFirstByOrgIdAndEfiscalSdcInvoicenoOrderByCreatedDesc(
                ORG_ID, "REF-OK")).thenReturn(Optional.of(ref));
        when(taxAuthorityService.call(eq(ORG_ID), eq("CREATE_INVOICE"), anyString()))
                .thenReturn(TA_SUCCESS_RESPONSE);

        FiscalBillService.ManualFiscalBillRequest request = copyRequest(
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00")),
                "REF-OK"
        );

        FiscalBillService.FiscalBillCreateResult result = fiscalBillService.createManualFiscalBill(
                ORG_ID, CLIENT_ID, "key-ref-ok", request);

        assertEquals(FiscalBillService.STATUS_SUCCESS, result.fiscalBill().status());
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(taxAuthorityService).call(eq(ORG_ID), eq("CREATE_INVOICE"), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("\"referentDocumentNumber\":\"REF-OK\""));
        assertTrue(bodyCaptor.getValue().contains("\"referentDocumentDT\":\"2024-05-01T12:00:00+02:00\""));
    }

    @Test
    void createManualFiscalBill_rejectsDuplicateOrderBill() {
        var existing = new FiscalBillEntity();
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

    @Test
    void createManualFiscalBill_rejectsEmptyItems() {
        FiscalBillService.ManualFiscalBillRequest request = manualRequest(
                null,
                List.of(),
                List.of(payment(1, "100.00"))
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-empty-items", request));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("No line items provided", ex.getReason());
    }

    @Test
    void createManualFiscalBill_rejectsEmptyPayments() {
        FiscalBillService.ManualFiscalBillRequest request = manualRequest(
                null,
                List.of(item("Product", "100.00")),
                List.of()
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-empty-payments", request));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("At least one payment is required", ex.getReason());
    }

    @Test
    void createManualFiscalBill_rejectsMixedTaxFieldsOnOrderChain() {
        FiscalBillService.FiscalBillItemRequest mixedItem = new FiscalBillService.FiscalBillItemRequest(
                "Product",
                BigDecimal.ONE,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("20"),
                null,
                null
        );

        FiscalBillEntity advance = advanceBill("ADV-1", "50.00");
        when(fiscalBillRepository.findLatestByOrgAndOrderAndType(
                ORG_ID, "ORD-ADV", FiscalBillService.INVOICE_TYPE_NORMAL, FiscalBillService.TRANSACTION_TYPE_SALE))
                .thenReturn(Optional.empty());
        when(fiscalBillRepository.findByOrgIdAndOrderIdAndInvoiceTypeAndTransactionType(
                ORG_ID, "ORD-ADV", FiscalBillService.INVOICE_TYPE_ADVANCE, FiscalBillService.TRANSACTION_TYPE_SALE))
                .thenReturn(List.of(advance));

        FiscalBillService.ManualFiscalBillRequest request = manualRequest(
                "ORD-ADV",
                List.of(mixedItem),
                List.of(payment(1, "100.00"))
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-mixed-tax", request));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("tax labels or order tax fields"));
        verify(taxAuthorityService, never()).call(anyLong(), anyString(), anyString());
    }

    @Test
    void createManualFiscalBill_closesAdvanceChainWithLabelOnlyItems() throws Exception {
        FiscalBillEntity advance = advanceBill("ADV-CHAIN-1", "100.00");
        when(fiscalBillRepository.findLatestByOrgAndOrderAndType(
                ORG_ID, "ORD-CHAIN", FiscalBillService.INVOICE_TYPE_NORMAL, FiscalBillService.TRANSACTION_TYPE_SALE))
                .thenReturn(Optional.empty());
        when(fiscalBillRepository.findByOrgIdAndOrderIdAndInvoiceTypeAndTransactionType(
                ORG_ID, "ORD-CHAIN", FiscalBillService.INVOICE_TYPE_ADVANCE, FiscalBillService.TRANSACTION_TYPE_SALE))
                .thenReturn(List.of(advance));
        stubTaxForAdvanceLabel("A");
        when(taxAuthorityService.call(eq(ORG_ID), eq("CREATE_INVOICE"), anyString()))
                .thenReturn(TA_SUCCESS_RESPONSE);

        FiscalBillService.ManualFiscalBillRequest request = manualRequest(
                "ORD-CHAIN",
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00"))
        );

        FiscalBillService.FiscalBillCreateResult result = fiscalBillService.createManualFiscalBill(
                ORG_ID, CLIENT_ID, "key-advance-chain", request);

        assertEquals(FiscalBillService.STATUS_SUCCESS, result.fiscalBill().status());
        verify(taxAuthorityService, times(2)).call(eq(ORG_ID), eq("CREATE_INVOICE"), anyString());
        verify(fiscalBillRepository, atLeastOnce()).save(any(FiscalBillEntity.class));

        ArgumentCaptor<FiscalBillLineEntity> lineCaptor = ArgumentCaptor.forClass(FiscalBillLineEntity.class);
        verify(fiscalBillLineRepository, atLeastOnce()).save(lineCaptor.capture());
        // Refund is the first persisted bill (id 100); Normal Sale is 101.
        boolean advanceRefundLineSaved = lineCaptor.getAllValues().stream()
                .anyMatch(line -> Long.valueOf(100L).equals(line.getFiscalbillId())
                        && "20 Advance (A)".equals(line.getName()));
        assertTrue(advanceRefundLineSaved, "Advance Refund must persist summarized fiscalbillline rows");

        ArgumentCaptor<FiscalBillPayEntity> payCaptor = ArgumentCaptor.forClass(FiscalBillPayEntity.class);
        verify(fiscalBillPayRepository, atLeastOnce()).save(payCaptor.capture());
        boolean advanceRefundPaySaved = payCaptor.getAllValues().stream()
                .anyMatch(pay -> Long.valueOf(100L).equals(pay.getFiscalbillId())
                        && pay.getAmount() != null
                        && pay.getAmount().compareTo(new BigDecimal("100.00")) == 0);
        assertTrue(advanceRefundPaySaved, "Advance Refund must persist fiscalbillpay rows");
    }

    @Test
    void createManualFiscalBill_omitsDateAndTimeOfIssueOutsideAdvanceSale() throws Exception {
        when(taxAuthorityService.call(eq(ORG_ID), eq("CREATE_INVOICE"), anyString()))
                .thenReturn(TA_SUCCESS_RESPONSE);

        FiscalBillService.ManualFiscalBillRequest request = manualRequest(
                null,
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00"))
        );

        fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-no-issue-date", request);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(taxAuthorityService).call(eq(ORG_ID), eq("CREATE_INVOICE"), bodyCaptor.capture());
        assertTrue(!bodyCaptor.getValue().contains("dateAndTimeOfIssue"),
                "Only Advance Sale bills may carry dateAndTimeOfIssue");
    }

    @Test
    void createManualFiscalBill_rejectsDateAndTimeOfIssueOutsideAdvanceSale() {
        FiscalBillService.ManualFiscalBillRequest request = new FiscalBillService.ManualFiscalBillRequest(
                null, null, null, false,
                FiscalBillService.INVOICE_TYPE_NORMAL,
                FiscalBillService.TRANSACTION_TYPE_SALE,
                null, null, null, null,
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00")),
                null,
                null,
                LocalDateTime.now().minusHours(1).toString()
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-issue-date-normal", request));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("dateAndTimeOfIssue is allowed only for Advance Sale fiscal bills", ex.getReason());
    }

    @Test
    void createManualFiscalBill_omitsDateAndTimeOfIssueWhenAdvanceMomentNotChosen() throws Exception {
        stubTaxForAdvanceLabel("A");
        when(taxAuthorityService.call(eq(ORG_ID), eq("CREATE_INVOICE"), anyString()))
                .thenReturn(TA_SUCCESS_RESPONSE);

        FiscalBillService.ManualFiscalBillRequest request = advanceSaleRequest(
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00")),
                null,
                null
        );

        fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-advance-no-moment", request);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(taxAuthorityService).call(eq(ORG_ID), eq("CREATE_INVOICE"), bodyCaptor.capture());
        assertTrue(!bodyCaptor.getValue().contains("dateAndTimeOfIssue"),
                "Advance Sale without a chosen moment must not default to the current time");

        ArgumentCaptor<FiscalBillEntity> entityCaptor = ArgumentCaptor.forClass(FiscalBillEntity.class);
        verify(fiscalBillRepository, atLeastOnce()).save(entityCaptor.capture());
        assertNull(entityCaptor.getValue().getDateAndTimeOfIssue());
    }

    @Test
    void createManualFiscalBill_sendsSelectedAdvancePaymentMoment() throws Exception {
        stubTaxForAdvanceLabel("A");
        when(taxAuthorityService.call(eq(ORG_ID), eq("CREATE_INVOICE"), anyString()))
                .thenReturn(TA_SUCCESS_RESPONSE);

        LocalDateTime advanceMoment = LocalDateTime.now(BELGRADE_ZONE).minusHours(2).withNano(0);
        FiscalBillService.ManualFiscalBillRequest request = advanceSaleRequest(
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00")),
                null,
                advanceMoment.toString()
        );

        fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-advance-moment", request);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(taxAuthorityService).call(eq(ORG_ID), eq("CREATE_INVOICE"), bodyCaptor.capture());
        String expected = advanceMoment.atZone(BELGRADE_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertTrue(bodyCaptor.getValue().contains("\"dateAndTimeOfIssue\":\"" + expected + "\""),
                "Advance Sale must send the selected advance payment moment, body: " + bodyCaptor.getValue());

        ArgumentCaptor<FiscalBillEntity> entityCaptor = ArgumentCaptor.forClass(FiscalBillEntity.class);
        verify(fiscalBillRepository, atLeastOnce()).save(entityCaptor.capture());
        assertEquals(expected, entityCaptor.getValue().getDateAndTimeOfIssue(),
                "The sent moment must be persisted for the PDF");
    }

    @Test
    void createManualFiscalBill_rejectsAdvancePaymentMomentOlderThanThreeDays() {
        FiscalBillService.ManualFiscalBillRequest request = advanceSaleRequest(
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00")),
                null,
                LocalDateTime.now(BELGRADE_ZONE).minusDays(4).toString()
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-advance-too-old", request));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("more than 3 days in the past"));
    }

    @Test
    void createManualFiscalBill_rejectsAdvancePaymentMomentInFuture() {
        FiscalBillService.ManualFiscalBillRequest request = advanceSaleRequest(
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00")),
                null,
                LocalDateTime.now(BELGRADE_ZONE).plusHours(1).toString()
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-advance-future", request));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("dateAndTimeOfIssue must not be in the future", ex.getReason());
    }

    @Test
    void createManualFiscalBill_sendsEsirNumberAsInvoiceNumber() throws Exception {
        when(taxAuthorityService.call(eq(ORG_ID), eq("CREATE_INVOICE"), anyString()))
                .thenReturn(TA_SUCCESS_RESPONSE);

        FiscalBillService.ManualFiscalBillRequest request = manualRequest(
                null,
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00"))
        );

        fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-esir", request);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(taxAuthorityService).call(eq(ORG_ID), eq("CREATE_INVOICE"), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("\"invoiceNumber\":\"123456/1.0.0\""));
    }

    @Test
    void createManualFiscalBill_sendsEmailWhenRequested() throws Exception {
        when(taxAuthorityService.call(eq(ORG_ID), eq("CREATE_INVOICE"), anyString()))
                .thenReturn(TA_SUCCESS_RESPONSE);

        FiscalBillService.ManualFiscalBillRequest request = new FiscalBillService.ManualFiscalBillRequest(
                null,
                "Customer",
                "customer@example.com",
                true,
                FiscalBillService.INVOICE_TYPE_NORMAL,
                FiscalBillService.TRANSACTION_TYPE_SALE,
                null,
                null,
                null,
                null,
                List.of(item("Product", "100.00")),
                List.of(payment(1, "100.00")),
                null,
                null,
                null
        );

        fiscalBillService.createManualFiscalBill(ORG_ID, CLIENT_ID, "key-email", request);

        verify(fiscalBillEmailService).sendIfRequested(
                eq(ORG_ID),
                any(FiscalBillEntity.class),
                eq(true),
                eq("customer@example.com"),
                eq("Customer"),
                eq(null)
        );
    }

    private void stubTaxForAdvanceLabel(String label) {
        TaxEntity tax = new TaxEntity();
        tax.setLabel(label);
        tax.setActive(true);
        tax.setEfiscalAdvanceprefix("20");
        tax.setEfiscalAdvancename("Advance");
        when(taxRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(tax));
    }

    private static FiscalBillEntity advanceBill(String invoiceNo, String total) {
        FiscalBillEntity advance = new FiscalBillEntity();
        advance.setFiscalbillId(50L);
        advance.setStatus(FiscalBillService.STATUS_SUCCESS);
        advance.setEfiscalSdcInvoiceno(invoiceNo);
        advance.setEfiscalSdcdatetime("2024-04-01T09:00:00+02:00");
        advance.setEfiscalTotalamount(new BigDecimal(total));
        return advance;
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
                null,
                null,
                items,
                payments,
                null,
                null,
                null
        );
    }

    private static FiscalBillService.ManualFiscalBillRequest advanceSaleRequest(
            List<FiscalBillService.FiscalBillItemRequest> items,
            List<FiscalBillService.PaymentRequest> payments,
            String referentDocumentNumber,
            String dateAndTimeOfIssue) {
        return new FiscalBillService.ManualFiscalBillRequest(
                null,
                null,
                null,
                false,
                FiscalBillService.INVOICE_TYPE_ADVANCE,
                FiscalBillService.TRANSACTION_TYPE_SALE,
                null,
                null,
                null,
                null,
                items,
                payments,
                referentDocumentNumber,
                null,
                dateAndTimeOfIssue
        );
    }

    private static FiscalBillService.ManualFiscalBillRequest copyRequest(
            List<FiscalBillService.FiscalBillItemRequest> items,
            List<FiscalBillService.PaymentRequest> payments,
            String referentDocumentNumber) {
        return new FiscalBillService.ManualFiscalBillRequest(
                null,
                null,
                null,
                false,
                FiscalBillService.INVOICE_TYPE_COPY,
                FiscalBillService.TRANSACTION_TYPE_SALE,
                null,
                null,
                null,
                null,
                items,
                payments,
                referentDocumentNumber,
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

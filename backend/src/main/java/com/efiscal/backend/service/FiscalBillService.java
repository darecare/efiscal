package com.efiscal.backend.service;

import com.efiscal.backend.model.FiscalBillConfigEntity;
import com.efiscal.backend.model.FiscalBillEntity;
import com.efiscal.backend.model.FiscalBillIdempotencyKeyEntity;
import com.efiscal.backend.model.FiscalBillLineEntity;
import com.efiscal.backend.model.FiscalBillPayEntity;
import com.efiscal.backend.model.FiscalBillTaxEntity;
import com.efiscal.backend.model.PayTypeMapEntity;
import com.efiscal.backend.model.TaxEntity;
import com.efiscal.backend.repository.FiscalBillConfigRepository;
import com.efiscal.backend.repository.FiscalBillIdempotencyKeyRepository;
import com.efiscal.backend.repository.FiscalBillLineRepository;
import com.efiscal.backend.repository.FiscalBillPayRepository;
import com.efiscal.backend.repository.FiscalBillRepository;
import com.efiscal.backend.repository.FiscalBillTaxRepository;
import com.efiscal.backend.repository.PayTypeMapRepository;
import com.efiscal.backend.repository.TaxRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fiscal Bill service implementing:
 * - 4.1 Order-Based Fiscalization (createFiscalBillFromOrder)
 * - 4.2 Manual Fiscal Bill Creation (createManualFiscalBill)
 * - Get Tax Authority Status
 * - Retry failed fiscal bills
 *
 * Business rules applied equally to order-based and manual creation:
 * - Header fields: invoiceType, transactionType, dateAndTime (Belgrade TZ), invoiceNumber from fiscalbillconfig
 * - Reference fields: based on previously issued fiscal bills (4.1.4)
 * - Advance closing chain: create Advance Refund before Normal Sale if Advance exists (4.1.5)
 * - Payment mapping: from paytype_map table per client (4.1.6)
 * - BuyerId: "10:" + company VAT if billing_type = company (4.1.7)
 * - Line items: normal (setLineItems) or advance (setAdvanceLineItems) depending on invoiceType (4.1.3)
 */
@Service
public class FiscalBillService {

    private static final Logger log = LoggerFactory.getLogger(FiscalBillService.class);

    /** Invoice type constants */
    public static final int INVOICE_TYPE_NORMAL = 0;
    public static final int INVOICE_TYPE_COPY = 2;
    public static final int INVOICE_TYPE_ADVANCE = 4;

    /** Transaction type constants */
    public static final int TRANSACTION_TYPE_SALE = 0;
    public static final int TRANSACTION_TYPE_REFUND = 1;

    /** Fiscal status strings */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_RETRYING = "RETRYING";

    private final FiscalBillRepository fiscalBillRepository;
    private final FiscalBillTaxRepository fiscalBillTaxRepository;
    private final FiscalBillPayRepository fiscalBillPayRepository;
    private final FiscalBillLineRepository fiscalBillLineRepository;
    private final FiscalBillIdempotencyKeyRepository idempotencyKeyRepository;
    private final FiscalBillConfigRepository fiscalBillConfigRepository;
    private final PayTypeMapRepository payTypeMapRepository;
    private final TaxRepository taxRepository;
    private final TaxAuthorityService taxAuthorityService;
    private final ObjectMapper objectMapper;

    public FiscalBillService(
            FiscalBillRepository fiscalBillRepository,
            FiscalBillTaxRepository fiscalBillTaxRepository,
            FiscalBillPayRepository fiscalBillPayRepository,
            FiscalBillLineRepository fiscalBillLineRepository,
            FiscalBillIdempotencyKeyRepository idempotencyKeyRepository,
            FiscalBillConfigRepository fiscalBillConfigRepository,
            PayTypeMapRepository payTypeMapRepository,
            TaxRepository taxRepository,
            TaxAuthorityService taxAuthorityService,
            ObjectMapper objectMapper) {
        this.fiscalBillRepository = fiscalBillRepository;
        this.fiscalBillTaxRepository = fiscalBillTaxRepository;
        this.fiscalBillPayRepository = fiscalBillPayRepository;
        this.fiscalBillLineRepository = fiscalBillLineRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.fiscalBillConfigRepository = fiscalBillConfigRepository;
        this.payTypeMapRepository = payTypeMapRepository;
        this.taxRepository = taxRepository;
        this.taxAuthorityService = taxAuthorityService;
        this.objectMapper = objectMapper;
    }

    // -----------------------------------------------------------------------
    // 4.1  Order-Based Fiscalization
    // -----------------------------------------------------------------------

    /**
     * Create a fiscal bill from a sales order.
     * Implements all rules from spec sections 4.1.1 – 4.1.8.
     *
     * @param orgId             organization id
     * @param clientId          client id (for payment type mapping lookup)
     * @param idempotencyKey    deduplication key
     * @param orderId           external order id
     * @param invoiceType       0=Normal, 4=Advance
     * @param transactionType   0=Sale, 1=Refund
     * @param orderData         raw order data from MerchantPro (used to build request)
     */
    @Transactional
    public FiscalBillCreateResult createFiscalBillFromOrder(
            Long orgId, Long clientId, String idempotencyKey,
            String orderId, int invoiceType, int transactionType,
            OrderFiscalizeRequest orderData) {

        // Idempotency check
        Optional<FiscalBillIdempotencyKeyEntity> existingKey = idempotencyKeyRepository.findById(idempotencyKey);
        if (existingKey.isPresent()) {
            return FiscalBillCreateResult.ofAlreadyExists(toView(existingKey.get().getFiscalBill()));
        }

        // Check duplicate (same order + invoiceType + transactionType)
        Optional<FiscalBillEntity> duplicate = fiscalBillRepository
                .findLatestByOrderAndType(orderId, invoiceType, transactionType);
        if (duplicate.isPresent() && STATUS_SUCCESS.equals(duplicate.get().getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Fiscal bill already exists for order " + orderId +
                    " with invoiceType=" + invoiceType + " transactionType=" + transactionType);
        }

        // Resolve order item tax labels before any dependent flow (including advance-refund chain).
        List<FiscalBillItemRequest> resolvedItems = resolveVatLabelsForOrderItems(orderData.items());

        // --- 4.1.5  Advance closing chain ---
        // If creating Normal Sale and Advance Sale exists → first create Advance Refund
        if (invoiceType == INVOICE_TYPE_NORMAL && transactionType == TRANSACTION_TYPE_SALE) {
            List<FiscalBillEntity> advanceBills = fiscalBillRepository
                    .findByOrderIdAndInvoiceTypeAndTransactionType(orderId, INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_SALE);
            if (!advanceBills.isEmpty()) {
                // Create Advance Refund to close the chain
            createAdvanceRefund(orgId, clientId, orderId, advanceBills, orderData, resolvedItems);
            }
        }

        // Build and send request
        FiscalBillConfigEntity config = resolveConfig(orgId);
        String requestBody = buildRequestBody(orgId, clientId, orderId, invoiceType, transactionType,
            resolvedItems, orderData.paymentMethodCode(), orderData.billingType(),
                orderData.billingCompanyVat(), config);

        FiscalBillEntity entity = createPendingEntity(orgId, clientId, orderId,
            invoiceType, transactionType, orderData.customerName(), requestBody);
        fiscalBillRepository.save(entity);
        registerIdempotencyKey(idempotencyKey, entity);

        try {
            String response = taxAuthorityService.call(orgId, "CREATE_INVOICE", requestBody);
            processTaxAuthorityResponse(entity, response, invoiceType, transactionType, resolvedItems, clientId, orgId);
            fiscalBillRepository.save(entity);
            // Save payment records
            savePaymentRecords(entity.getFiscalbillId(), clientId, orgId, orderData.paymentMethodCode(),
                    entity.getEfiscalTotalamount());
            // Save line items after successful fiscalization
                saveLineItems(entity.getFiscalbillId(), clientId, orgId, resolvedItems);
        } catch (ResponseStatusException rse) {
            entity.setStatus(STATUS_FAILED);
            entity.setLastError(rse.getReason());
            entity.setUpdated(LocalDateTime.now());
            fiscalBillRepository.save(entity);
            return FiscalBillCreateResult.ofFailed(toView(entity));
        } catch (Exception ex) {
            entity.setStatus(STATUS_FAILED);
            entity.setLastError(ex.getMessage());
            entity.setUpdated(LocalDateTime.now());
            fiscalBillRepository.save(entity);
            return FiscalBillCreateResult.ofFailed(toView(entity));
        }

        return FiscalBillCreateResult.ofCreated(toView(entity));
    }

    // -----------------------------------------------------------------------
    // 4.2  Manual Fiscal Bill Creation
    // -----------------------------------------------------------------------

    /**
     * Create a fiscal bill manually.
     * Applies the same business rules as order-based creation (spec 4.2).
     *
     * @param orgId           organization id
     * @param clientId        client id
     * @param idempotencyKey  deduplication key
     * @param request         manual fiscal bill request
     */
    @Transactional
    public FiscalBillCreateResult createManualFiscalBill(
            Long orgId, Long clientId, String idempotencyKey,
            ManualFiscalBillRequest request) {

        // Idempotency check
        Optional<FiscalBillIdempotencyKeyEntity> existingKey = idempotencyKeyRepository.findById(idempotencyKey);
        if (existingKey.isPresent()) {
            return FiscalBillCreateResult.ofAlreadyExists(toView(existingKey.get().getFiscalBill()));
        }

        // If an orderId is provided, apply the same checks as order-based fiscalization (spec 4.2.1)
        String orderId = request.orderId();

        // Check duplicate if orderId is set
        if (orderId != null && !orderId.isBlank()) {
            Optional<FiscalBillEntity> duplicate = fiscalBillRepository
                    .findLatestByOrderAndType(orderId, request.invoiceType(), request.transactionType());
            if (duplicate.isPresent() && STATUS_SUCCESS.equals(duplicate.get().getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Fiscal bill already exists for order " + orderId);
            }

            // 4.1.5 Advance closing chain (also applies when orderId is provided in manual creation)
            if (request.invoiceType() == INVOICE_TYPE_NORMAL && request.transactionType() == TRANSACTION_TYPE_SALE) {
                List<FiscalBillEntity> advanceBills = fiscalBillRepository
                        .findByOrderIdAndInvoiceTypeAndTransactionType(orderId, INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_SALE);
                if (!advanceBills.isEmpty()) {
                    OrderFiscalizeRequest syntheticOrder = buildSyntheticOrderFromManual(request);
                    createAdvanceRefund(orgId, clientId, orderId, advanceBills, syntheticOrder,
                            resolveVatLabelsForOrderItems(request.items()));
                }
            }
        }

        FiscalBillConfigEntity config = resolveConfig(orgId);

        // Build request body — manual items, manual payments
        String requestBody = buildManualRequestBody(orgId, clientId, orderId,
                request.invoiceType(), request.transactionType(),
                request.items(), request.payments(),
                request.buyerType(), request.buyerVat(), config);

        FiscalBillEntity entity = createPendingEntity(orgId, clientId, orderId,
            request.invoiceType(), request.transactionType(), request.customerName(), requestBody);
        fiscalBillRepository.save(entity);
        registerIdempotencyKey(idempotencyKey, entity);

        try {
            String response = taxAuthorityService.call(orgId, "CREATE_INVOICE", requestBody);
            processTaxAuthorityResponse(entity, response, request.invoiceType(), request.transactionType(),
                    request.items(), clientId, orgId);
            fiscalBillRepository.save(entity);
            // Save payment records from manual payment rows
            saveManualPaymentRecords(entity.getFiscalbillId(), clientId, orgId, request.payments());
            // Save line items
            saveLineItems(entity.getFiscalbillId(), clientId, orgId, request.items());
        } catch (ResponseStatusException rse) {
            entity.setStatus(STATUS_FAILED);
            entity.setLastError(rse.getReason());
            entity.setUpdated(LocalDateTime.now());
            fiscalBillRepository.save(entity);
            return FiscalBillCreateResult.ofFailed(toView(entity));
        } catch (Exception ex) {
            entity.setStatus(STATUS_FAILED);
            entity.setLastError(ex.getMessage());
            entity.setUpdated(LocalDateTime.now());
            fiscalBillRepository.save(entity);
            return FiscalBillCreateResult.ofFailed(toView(entity));
        }

        return FiscalBillCreateResult.ofCreated(toView(entity));
    }

    // -----------------------------------------------------------------------
    // Get Tax Authority Status
    // -----------------------------------------------------------------------

    public Map<String, Object> getStatus(Long orgId) {
        String responseBody = taxAuthorityService.call(orgId, "GET_STATUS", null);
        try {
            return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to parse Tax Authority response: " + ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Retry
    // -----------------------------------------------------------------------

    @Transactional
    public FiscalBillRetryResult retryFiscalBill(Long fiscalBillId, String idempotencyKey) {
        Optional<FiscalBillIdempotencyKeyEntity> existingRetryKey = idempotencyKeyRepository.findById(idempotencyKey);
        if (existingRetryKey.isPresent()) {
            Long existingBillId = existingRetryKey.get().getFiscalBill().getFiscalbillId();
            if (!existingBillId.equals(fiscalBillId)) {
                return FiscalBillRetryResult.ofIdempotencyConflict();
            }
        }

        FiscalBillEntity entity = fiscalBillRepository.findById(fiscalBillId).orElse(null);
        if (entity == null) return FiscalBillRetryResult.ofNotFound();
        if (!STATUS_FAILED.equals(entity.getStatus())) return FiscalBillRetryResult.ofNotRetryable(toView(entity));

        entity.setStatus(STATUS_RETRYING);
        entity.setLastError(null);
        entity.setAttemptCount(entity.getAttemptCount() == null ? 1 : entity.getAttemptCount() + 1);
        entity.setUpdated(LocalDateTime.now());
        fiscalBillRepository.save(entity);

        if (existingRetryKey.isEmpty()) {
            registerIdempotencyKey(idempotencyKey, entity);
        }
        return FiscalBillRetryResult.ofRetried(toView(entity));
    }

    @Transactional
    public FiscalBillCreateResult createCopyFiscalBill(Long sourceFiscalBillId, String idempotencyKey) {
        Optional<FiscalBillIdempotencyKeyEntity> existingKey = idempotencyKeyRepository.findById(idempotencyKey);
        if (existingKey.isPresent()) {
            return FiscalBillCreateResult.ofAlreadyExists(toView(existingKey.get().getFiscalBill()));
        }

        FiscalBillEntity source = fiscalBillRepository.findById(sourceFiscalBillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source fiscal bill not found"));

        if (!STATUS_SUCCESS.equals(source.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only SUCCESS fiscal bills can be used to create Copy");
        }
        if (source.getEfiscalSdcInvoiceno() == null || source.getEfiscalSdcInvoiceno().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Source fiscal bill is missing Tax Authority invoice number");
        }

        List<FiscalBillLineEntity> sourceLines = fiscalBillLineRepository.findByFiscalbillId(sourceFiscalBillId);
        if (sourceLines.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Source fiscal bill has no line items to copy");
        }

        List<FiscalBillItemRequest> copyItems = sourceLines.stream()
                .map(this::toCopyItemRequest)
                .toList();

        List<PaymentRequest> copyPayments = fiscalBillPayRepository.findByFiscalbillId(sourceFiscalBillId).stream()
                .map(p -> new PaymentRequest(p.getPaymentType(), p.getAmount()))
                .toList();
        if (copyPayments.isEmpty()) {
            if (source.getEfiscalTotalamount() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Source fiscal bill has no payment rows and no total amount for fallback payment");
            }
            copyPayments = List.of(new PaymentRequest(0, source.getEfiscalTotalamount()));
        }

        FiscalBillConfigEntity config = resolveConfig(source.getOrgId());
        String requestBody = buildCopyRequestBody(source, copyItems, copyPayments, config);

        int copyTransactionType = source.getEfiscalTransactiontype() == null
                ? TRANSACTION_TYPE_SALE
                : source.getEfiscalTransactiontype();

        FiscalBillEntity entity = createPendingEntity(
                source.getOrgId(),
                source.getClientId(),
                source.getOrderId(),
                INVOICE_TYPE_COPY,
                copyTransactionType,
                source.getEfiscalCustomername(),
                requestBody);
        fiscalBillRepository.save(entity);
        registerIdempotencyKey(idempotencyKey, entity);

        try {
            String response = taxAuthorityService.call(source.getOrgId(), "CREATE_INVOICE", requestBody);
            processTaxAuthorityResponse(entity, response, INVOICE_TYPE_COPY, copyTransactionType,
                    copyItems, source.getClientId(), source.getOrgId());
            fiscalBillRepository.save(entity);
            saveManualPaymentRecords(entity.getFiscalbillId(), source.getClientId(), source.getOrgId(), copyPayments);
            saveLineItems(entity.getFiscalbillId(), source.getClientId(), source.getOrgId(), copyItems);
        } catch (ResponseStatusException rse) {
            entity.setStatus(STATUS_FAILED);
            entity.setLastError(rse.getReason());
            entity.setUpdated(LocalDateTime.now());
            fiscalBillRepository.save(entity);
            return FiscalBillCreateResult.ofFailed(toView(entity));
        } catch (Exception ex) {
            entity.setStatus(STATUS_FAILED);
            entity.setLastError(ex.getMessage());
            entity.setUpdated(LocalDateTime.now());
            fiscalBillRepository.save(entity);
            return FiscalBillCreateResult.ofFailed(toView(entity));
        }

        return FiscalBillCreateResult.ofCreated(toView(entity));
    }

    @Transactional
    public FiscalBillCreateResult createRefundFiscalBill(Long sourceFiscalBillId, String idempotencyKey) {
        Optional<FiscalBillIdempotencyKeyEntity> existingKey = idempotencyKeyRepository.findById(idempotencyKey);
        if (existingKey.isPresent()) {
            return FiscalBillCreateResult.ofAlreadyExists(toView(existingKey.get().getFiscalBill()));
        }

        FiscalBillEntity source = fiscalBillRepository.findById(sourceFiscalBillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source fiscal bill not found"));

        if (!STATUS_SUCCESS.equals(source.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only SUCCESS fiscal bills can be used to create Refund");
        }
        if (source.getEfiscalTransactiontype() == null || source.getEfiscalTransactiontype() != TRANSACTION_TYPE_SALE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Create Refund is allowed only for Sale fiscal bills");
        }
        if (source.getEfiscalInvoicetype() != null && source.getEfiscalInvoicetype() == INVOICE_TYPE_COPY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Create Refund is not allowed for Copy fiscal bills");
        }
        if (source.getEfiscalSdcInvoiceno() == null || source.getEfiscalSdcInvoiceno().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Source fiscal bill is missing Tax Authority invoice number");
        }

        List<FiscalBillLineEntity> sourceLines = fiscalBillLineRepository.findByFiscalbillId(sourceFiscalBillId);
        if (sourceLines.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Source fiscal bill has no line items to refund");
        }

        List<FiscalBillItemRequest> refundItems = sourceLines.stream()
                .map(this::toCopyItemRequest)
                .toList();

        List<PaymentRequest> refundPayments = fiscalBillPayRepository.findByFiscalbillId(sourceFiscalBillId).stream()
                .map(p -> new PaymentRequest(p.getPaymentType(), p.getAmount()))
                .toList();
        if (refundPayments.isEmpty()) {
            if (source.getEfiscalTotalamount() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Source fiscal bill has no payment rows and no total amount for fallback payment");
            }
            refundPayments = List.of(new PaymentRequest(0, source.getEfiscalTotalamount()));
        }

        FiscalBillConfigEntity config = resolveConfig(source.getOrgId());
        String requestBody = buildRefundRequestBody(source, refundItems, refundPayments, config);

        int refundInvoiceType = source.getEfiscalInvoicetype() == null
                ? INVOICE_TYPE_NORMAL
                : source.getEfiscalInvoicetype();

        FiscalBillEntity entity = createPendingEntity(
                source.getOrgId(),
                source.getClientId(),
                source.getOrderId(),
                refundInvoiceType,
                TRANSACTION_TYPE_REFUND,
                source.getEfiscalCustomername(),
                requestBody);
        fiscalBillRepository.save(entity);
        registerIdempotencyKey(idempotencyKey, entity);

        try {
            String response = taxAuthorityService.call(source.getOrgId(), "CREATE_INVOICE", requestBody);
            processTaxAuthorityResponse(entity, response, refundInvoiceType, TRANSACTION_TYPE_REFUND,
                    refundItems, source.getClientId(), source.getOrgId());
            fiscalBillRepository.save(entity);
            saveManualPaymentRecords(entity.getFiscalbillId(), source.getClientId(), source.getOrgId(), refundPayments);
            saveLineItems(entity.getFiscalbillId(), source.getClientId(), source.getOrgId(), refundItems);
        } catch (ResponseStatusException rse) {
            entity.setStatus(STATUS_FAILED);
            entity.setLastError(rse.getReason());
            entity.setUpdated(LocalDateTime.now());
            fiscalBillRepository.save(entity);
            return FiscalBillCreateResult.ofFailed(toView(entity));
        } catch (Exception ex) {
            entity.setStatus(STATUS_FAILED);
            entity.setLastError(ex.getMessage());
            entity.setUpdated(LocalDateTime.now());
            fiscalBillRepository.save(entity);
            return FiscalBillCreateResult.ofFailed(toView(entity));
        }

        return FiscalBillCreateResult.ofCreated(toView(entity));
    }

    @Transactional(readOnly = true)
    public FiscalBillView findFiscalBillById(Long fiscalBillId) {
        return fiscalBillRepository.findById(fiscalBillId).map(this::toView).orElse(null);
    }

        @Transactional(readOnly = true)
        public List<FiscalBillListView> listFiscalBills(Long orgId) {
        return fiscalBillRepository.findByOrgIdOrderByCreatedDesc(orgId).stream()
            .map(this::toListView)
            .toList();
        }

        @Transactional(readOnly = true)
        public FiscalBillDetailsView findFiscalBillDetails(Long fiscalBillId) {
        FiscalBillEntity entity = fiscalBillRepository.findById(fiscalBillId).orElse(null);
        if (entity == null) {
            return null;
        }
        List<FiscalBillTaxView> taxItems = fiscalBillTaxRepository.findByFiscalbillId(fiscalBillId).stream()
            .map(tax -> new FiscalBillTaxView(
                tax.getFiscalbilltaxId(),
                tax.getEfiscalTaxlabel(),
                tax.getEfiscalCategoryname(),
                tax.getEfiscalCategorytype(),
                tax.getRate(),
                tax.getAmount()))
            .toList();
        List<FiscalBillLineView> lineItems = fiscalBillLineRepository.findByFiscalbillId(fiscalBillId).stream()
            .map(line -> new FiscalBillLineView(
                line.getFiscalbilllineId(),
                line.getName(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getTotalAmount(),
                line.getTaxLabel(),
                line.getGtin(),
                line.getProductId(),
                line.getSku()))
            .toList();
        List<FiscalBillPayView> payments = fiscalBillPayRepository.findByFiscalbillId(fiscalBillId).stream()
            .map(pay -> new FiscalBillPayView(
                pay.getFiscalbillpayId(),
                pay.getPaymentType(),
                pay.getAmount()))
            .toList();
        return new FiscalBillDetailsView(toView(entity), taxItems, lineItems, payments);
        }

    // -----------------------------------------------------------------------
    // Private helpers — request building
    // -----------------------------------------------------------------------

    /**
     * Build Tax Authority request body for order-based fiscalization.
     */
    private String buildRequestBody(Long orgId, Long clientId, String orderId,
            int invoiceType, int transactionType,
            List<FiscalBillItemRequest> items, String paymentMethodCode,
            String billingType, String billingCompanyVat,
            FiscalBillConfigEntity config) {

        Map<String, Object> body = new HashMap<>();

        // Header (4.1.2)
        body.put("invoiceType", invoiceType);
        body.put("transactionType", transactionType);
        body.put("dateAndTimeOfIssue", belgradeNow());
        if (config != null && config.getEsirno() != null) {
            body.put("invoiceNumber", config.getEsirno());
        }

        // BuyerId (4.1.7)
        String buyerId = resolveBuyerIdFromOrder(billingType, billingCompanyVat);
        if (buyerId != null) body.put("buyerId", buyerId);

        // Reference fields (4.1.4)
        setReferentFields(body, orderId, invoiceType, transactionType);

        // Payment (4.1.6)
        body.put("payment", buildPaymentArrayFromCode(clientId, paymentMethodCode,
                items.stream().map(FiscalBillItemRequest::totalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)));

        // Items (4.1.3)
        if (invoiceType == INVOICE_TYPE_ADVANCE) {
            body.put("items", buildAdvanceLineItems(items));
        } else {
            body.put("items", buildLineItems(items));
        }

        return toJson(body);
    }

    /**
     * Build Tax Authority request body for manual fiscalization.
     */
    private String buildManualRequestBody(Long orgId, Long clientId, String orderId,
            int invoiceType, int transactionType,
            List<FiscalBillItemRequest> items, List<PaymentRequest> payments,
            String buyerType, String buyerVat,
            FiscalBillConfigEntity config) {

        Map<String, Object> body = new HashMap<>();

        // Header (4.1.2 / 4.2.1)
        body.put("invoiceType", invoiceType);
        body.put("transactionType", transactionType);
        body.put("dateAndTimeOfIssue", belgradeNow());
        if (config != null && config.getEsirno() != null) {
            body.put("invoiceNumber", config.getEsirno());
        }

        // BuyerId (4.2.1)
        if (buyerType != null && buyerVat != null && !buyerVat.isBlank()) {
            body.put("buyerId", buyerType + ":" + buyerVat);
        }

        // Reference fields (4.1.4) — applies when orderId is set
        if (orderId != null && !orderId.isBlank()) {
            setReferentFields(body, orderId, invoiceType, transactionType);
        }

        // Payment from manually entered payment rows (4.2.3)
        body.put("payment", buildPaymentArrayFromRows(payments));

        // Items (4.1.3)
        if (invoiceType == INVOICE_TYPE_ADVANCE) {
            body.put("items", buildAdvanceLineItems(items));
        } else {
            body.put("items", buildLineItems(items));
        }

        return toJson(body);
    }

    private String buildCopyRequestBody(
            FiscalBillEntity source,
            List<FiscalBillItemRequest> items,
            List<PaymentRequest> payments,
            FiscalBillConfigEntity config) {

        Map<String, Object> body = new HashMap<>();
        int sourceTransactionType = source.getEfiscalTransactiontype() == null
                ? TRANSACTION_TYPE_SALE
                : source.getEfiscalTransactiontype();

        body.put("invoiceType", INVOICE_TYPE_COPY);
        body.put("transactionType", sourceTransactionType);
        body.put("dateAndTimeOfIssue", belgradeNow());
        if (config != null && config.getEsirno() != null) {
            body.put("invoiceNumber", config.getEsirno());
        }

        body.put("referentDocumentNumber", source.getEfiscalSdcInvoiceno());
        if (source.getEfiscalSdcdatetime() != null && !source.getEfiscalSdcdatetime().isBlank()) {
            body.put("referentDocumentDT", source.getEfiscalSdcdatetime());
        }

        body.put("payment", buildPaymentArrayFromRows(payments));
        if (source.getEfiscalInvoicetype() != null && source.getEfiscalInvoicetype() == INVOICE_TYPE_ADVANCE) {
            body.put("items", buildAdvanceLineItems(items));
        } else {
            body.put("items", buildLineItems(items));
        }
        return toJson(body);
    }

    private String buildRefundRequestBody(
            FiscalBillEntity source,
            List<FiscalBillItemRequest> items,
            List<PaymentRequest> payments,
            FiscalBillConfigEntity config) {

        Map<String, Object> body = new HashMap<>();
        int refundInvoiceType = source.getEfiscalInvoicetype() == null
                ? INVOICE_TYPE_NORMAL
                : source.getEfiscalInvoicetype();

        body.put("invoiceType", refundInvoiceType);
        body.put("transactionType", TRANSACTION_TYPE_REFUND);
        body.put("dateAndTimeOfIssue", belgradeNow());
        if (config != null && config.getEsirno() != null) {
            body.put("invoiceNumber", config.getEsirno());
        }

        body.put("referentDocumentNumber", source.getEfiscalSdcInvoiceno());
        if (source.getEfiscalSdcdatetime() != null && !source.getEfiscalSdcdatetime().isBlank()) {
            body.put("referentDocumentDT", source.getEfiscalSdcdatetime());
        }

        body.put("payment", buildPaymentArrayFromRows(payments));
        if (refundInvoiceType == INVOICE_TYPE_ADVANCE) {
            body.put("items", buildAdvanceLineItems(items));
        } else {
            body.put("items", buildLineItems(items));
        }
        return toJson(body);
    }

    /**
     * Normal line items array — one entry per product item (4.1.3 rule 1).
     */
    private List<Map<String, Object>> buildLineItems(List<FiscalBillItemRequest> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (FiscalBillItemRequest item : items) {
            Map<String, Object> line = new HashMap<>();
            line.put("name", item.name());
            line.put("quantity", item.quantity());
            line.put("unitPrice", item.unitPrice());
            line.put("totalAmount", item.totalAmount());
            line.put("labels", resolveItemLabels(item));
            if (item.gtin() != null && !item.gtin().isBlank()) {
                line.put("gtin", item.gtin());
            }
            result.add(line);
        }
        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No line items provided");
        }
        return result;
    }

    /**
     * Advance line items — summarized per tax rate (4.1.3 rule 2).
     * Name is resolved from tax table: efiscal_advanceprefix + efiscal_advancename.
     */
    private List<Map<String, Object>> buildAdvanceLineItems(List<FiscalBillItemRequest> items) {
        // Group items by tax label and sum totalAmount.
        Map<String, BigDecimal> groupedByLabel = new HashMap<>();
        for (FiscalBillItemRequest item : items) {
            String label = resolvePrimaryLabel(item);
            groupedByLabel.merge(label, item.totalAmount(), BigDecimal::add);
        }
        if (groupedByLabel.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No line items for advance invoice");
        }

        Map<String, String> advanceNameByLabel = resolveAdvanceNameByLabel(groupedByLabel.keySet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : groupedByLabel.entrySet()) {
            String label = entry.getKey();
            BigDecimal total = entry.getValue();
            Map<String, Object> line = new HashMap<>();
            line.put("name", advanceNameByLabel.get(label));
            line.put("quantity", BigDecimal.ONE);
            line.put("unitPrice", total);
            line.put("totalAmount", total);
            line.put("labels", List.of(label));
            result.add(line);
        }
        return result;
    }

    private Map<String, String> resolveAdvanceNameByLabel(Set<String> labels) {
        Map<String, List<TaxEntity>> activeTaxesByLabel = taxRepository.findAllByDeletedAtIsNull().stream()
                .filter(TaxEntity::isActive)
                .filter(t -> t.getLabel() != null && !t.getLabel().isBlank())
                .collect(Collectors.groupingBy(t -> t.getLabel().trim().toUpperCase()));

        Map<String, String> resolved = new HashMap<>();
        for (String label : labels) {
            String normalizedLabel = label == null ? null : label.trim().toUpperCase();
            List<TaxEntity> matches = normalizedLabel == null ? List.of() : activeTaxesByLabel.get(normalizedLabel);
            if (matches == null || matches.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No active tax configuration found for advance line label '" + label + "'");
            }
            if (matches.size() > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Multiple active tax rows found for advance line label '" + label
                                + "'. Configure a unique active label.");
            }

            TaxEntity tax = matches.get(0);
            if (tax.getEfiscalAdvanceprefix() == null || tax.getEfiscalAdvanceprefix().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tax label '" + label + "' is missing efiscal_advanceprefix");
            }
            if (tax.getEfiscalAdvancename() == null || tax.getEfiscalAdvancename().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tax label '" + label + "' is missing efiscal_advancename");
            }

            resolved.put(label, tax.getEfiscalAdvanceprefix().trim() + tax.getEfiscalAdvancename().trim());
        }
        return resolved;
    }

    /**
     * Set referentDocumentNumber and referentDocumentDT fields (4.1.4).
     */
    private void setReferentFields(Map<String, Object> body, String orderId,
            int invoiceType, int transactionType) {
        FiscalBillEntity ref = null;

        if (invoiceType == INVOICE_TYPE_NORMAL && transactionType == TRANSACTION_TYPE_SALE) {
            // Normal Sale references to last issued Advance Refund (closes advance chain)
            ref = fiscalBillRepository
                    .findLatestByOrderAndType(orderId, INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_REFUND)
                    .orElse(null);
        } else if (invoiceType == INVOICE_TYPE_NORMAL && transactionType == TRANSACTION_TYPE_REFUND) {
            // Normal Refund references to Normal Sale
            ref = fiscalBillRepository
                    .findLatestByOrderAndType(orderId, INVOICE_TYPE_NORMAL, TRANSACTION_TYPE_SALE)
                    .orElse(null);
        } else if (invoiceType == INVOICE_TYPE_ADVANCE && transactionType == TRANSACTION_TYPE_SALE) {
            // Advance Sale can reference to last Advance Sale (for chained advances)
            ref = fiscalBillRepository
                    .findLatestByOrderAndType(orderId, INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_SALE)
                    .orElse(null);
        }

        if (ref != null && ref.getEfiscalSdcInvoiceno() != null) {
            body.put("referentDocumentNumber", ref.getEfiscalSdcInvoiceno());
            if (ref.getEfiscalSdcdatetime() != null) {
                body.put("referentDocumentDT", ref.getEfiscalSdcdatetime());
            }
        }
    }

    /**
     * Build payment array from order payment_method_code using paytype_map table (4.1.6).
     * Falls back to Other (0) if no mapping exists.
     */
    private List<Map<String, Object>> buildPaymentArrayFromCode(Long clientId, String paymentMethodCode,
            BigDecimal totalAmount) {
        int paymentType = resolvePaymentType(clientId, paymentMethodCode);
        Map<String, Object> payment = new HashMap<>();
        payment.put("amount", totalAmount);
        payment.put("paymentType", paymentType);
        return List.of(payment);
    }

    /**
     * Build payment array from manually entered payment rows (4.2.3).
     */
    private List<Map<String, Object>> buildPaymentArrayFromRows(List<PaymentRequest> payments) {
        if (payments == null || payments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one payment is required");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (PaymentRequest p : payments) {
            Map<String, Object> pay = new HashMap<>();
            pay.put("amount", p.amount());
            pay.put("paymentType", p.paymentType());
            result.add(pay);
        }
        return result;
    }

    /**
     * Resolve fiscal payment type integer from paytype_map.
     * Falls back to 0 (Other) if no mapping found.
     */
    private int resolvePaymentType(Long clientId, String paymentMethodCode) {
        if (paymentMethodCode == null || paymentMethodCode.isBlank()) return 0;
        String normalizedPaymentMethodCode = paymentMethodCode.trim();
        return payTypeMapRepository.findByClientId(clientId).stream()
            .filter(mapping -> "Y".equalsIgnoreCase(mapping.getIsactive()))
            .filter(mapping -> mapping.getPaymentMethodCode() != null)
            .filter(mapping -> mapping.getPaymentMethodCode().trim().equalsIgnoreCase(normalizedPaymentMethodCode))
            .map(PayTypeMapEntity::getPaymentType)
            .findFirst()
            .orElse(0);
    }

    /**
     * Build buyerId for order-based fiscal bill (4.1.7).
     * Format: "10:" + billing_company_vat when billing_type = "company"
     */
    private String resolveBuyerIdFromOrder(String billingType, String billingCompanyVat) {
        if ("company".equalsIgnoreCase(billingType)
                && billingCompanyVat != null && !billingCompanyVat.isBlank()) {
            return "10:" + billingCompanyVat;
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // 4.1.5  Advance closing chain
    // -----------------------------------------------------------------------

        private void createAdvanceRefund(Long orgId, Long clientId, String orderId,
            List<FiscalBillEntity> advanceBills, OrderFiscalizeRequest orderData,
            List<FiscalBillItemRequest> resolvedItems) {
        // Summarize all previous Advance Normal amounts
        BigDecimal totalAdvanceAmount = advanceBills.stream()
                .filter(b -> STATUS_SUCCESS.equals(b.getStatus()))
                .map(b -> b.getEfiscalTotalamount() != null ? b.getEfiscalTotalamount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAdvanceAmount.compareTo(BigDecimal.ZERO) == 0) return;

        // Get the last advance bill as reference
        FiscalBillEntity lastAdvance = advanceBills.get(0);

        FiscalBillConfigEntity config = resolveConfig(orgId);

        // Build refund items that mirror the advance items but as refund
        List<FiscalBillItemRequest> refundItems = buildAdvanceRefundItems(orderId, totalAdvanceAmount, resolvedItems);

        Map<String, Object> body = new HashMap<>();
        body.put("invoiceType", INVOICE_TYPE_ADVANCE);
        body.put("transactionType", TRANSACTION_TYPE_REFUND);
        body.put("dateAndTimeOfIssue", belgradeNow());
        if (config != null && config.getEsirno() != null) {
            body.put("invoiceNumber", config.getEsirno());
        }
        // Reference the last advance bill
        if (lastAdvance.getEfiscalSdcInvoiceno() != null) {
            body.put("referentDocumentNumber", lastAdvance.getEfiscalSdcInvoiceno());
            if (lastAdvance.getEfiscalSdcdatetime() != null) {
                body.put("referentDocumentDT", lastAdvance.getEfiscalSdcdatetime());
            }
        }
        body.put("payment", buildPaymentArrayFromCode(clientId,
                orderData.paymentMethodCode(), totalAdvanceAmount));
        body.put("items", buildAdvanceLineItems(refundItems));

        String advanceRefundRequestBody = toJson(body);

        FiscalBillEntity refundEntity = createPendingEntity(orgId, clientId, orderId,
            INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_REFUND, orderData.customerName(), advanceRefundRequestBody);
        fiscalBillRepository.save(refundEntity);

        try {
            String response = taxAuthorityService.call(orgId, "CREATE_INVOICE", advanceRefundRequestBody);
            processTaxAuthorityResponse(refundEntity, response, INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_REFUND,
                    refundItems, clientId, orgId);
            fiscalBillRepository.save(refundEntity);
            log.info("Advance Refund created: {}", refundEntity.getFiscalbillId());
        } catch (Exception ex) {
            log.error("Failed to create Advance Refund for order {}", orderId, ex);
            refundEntity.setStatus(STATUS_FAILED);
            refundEntity.setLastError("Advance Refund failed: " + ex.getMessage());
            refundEntity.setUpdated(LocalDateTime.now());
            fiscalBillRepository.save(refundEntity);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to create required Advance Refund: " + ex.getMessage());
        }
    }

    private List<FiscalBillItemRequest> buildAdvanceRefundItems(String orderId,
            BigDecimal totalAmount, List<FiscalBillItemRequest> resolvedItems) {
        // Use resolved order items so labels are always available for advance summarization.
        return resolvedItems;
    }

    private FiscalBillItemRequest toCopyItemRequest(FiscalBillLineEntity line) {
        String taxLabel = line.getTaxLabel() == null ? null : line.getTaxLabel().trim();
        List<String> labels = (taxLabel == null || taxLabel.isBlank()) ? null : List.of(taxLabel);
        return new FiscalBillItemRequest(
                line.getName(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getTotalAmount(),
                taxLabel,
                null,
                line.getGtin(),
                line.getProductId(),
                line.getSku(),
                null,
                null,
                labels);
    }

    // -----------------------------------------------------------------------
    // Response processing & persistence
    // -----------------------------------------------------------------------

    private void processTaxAuthorityResponse(FiscalBillEntity entity, String responseBody,
            int invoiceType, int transactionType,
            List<FiscalBillItemRequest> items,
            Long clientId, Long orgId) throws Exception {

        Map<String, Object> resp = objectMapper.readValue(responseBody, new TypeReference<>() {});

        entity.setStatus(STATUS_SUCCESS);
        entity.setEfiscalSdcInvoiceno(str(resp.get("invoiceNumber")));
        entity.setEfiscalSdcdatetime(str(resp.get("sdcDateTime")));
        entity.setEfiscalLink(str(resp.get("verificationUrl")));
        entity.setEfiscalQr(str(resp.get("verificationQRCode")));
        entity.setEfiscalRequestedby(str(resp.get("requestedBy")));
        entity.setEfiscalSignedby(str(resp.get("signedBy")));
        entity.setEfiscalInvoicecounter(str(resp.get("invoiceCounter")));
        entity.setEfiscalInvoicecounterext(str(resp.get("invoiceCounterExtension")));
        entity.setEfiscalEncryptedinternaldata(str(resp.get("encryptedInternalData")));
        entity.setEfiscalSignature(str(resp.get("signature")));
        entity.setEfiscalMessages(trimTo(str(resp.get("messages")), 22));
        entity.setEfiscalBusinessname(str(resp.get("businessName")));
        entity.setEfiscalTin(str(resp.get("tin")));
        entity.setEfiscalAddress(str(resp.get("address")));
        entity.setEfiscalMrc(str(resp.get("mrc")));
        entity.setEfiscalInvoicetype(invoiceType);
        entity.setEfiscalTransactiontype(transactionType);
        entity.setUpdated(LocalDateTime.now());
        entity.setProviderReference(str(resp.get("invoiceNumber")));

        if (resp.get("totalAmount") != null) {
            entity.setEfiscalTotalamount(new BigDecimal(str(resp.get("totalAmount"))));
        }
        if (resp.get("transactionTypeCounter") != null) {
            entity.setEfiscalTransactiontypecounter(((Number) resp.get("transactionTypeCounter")).longValue());
        }
        if (resp.get("taxGroupRevision") != null) {
            entity.setEfiscalTaxgrouprevision(((Number) resp.get("taxGroupRevision")).longValue());
        }

        // Save tax items from taxItems array in response
        Object taxItemsObj = resp.get("taxItems");
        if (taxItemsObj instanceof List<?> taxItemsList) {
            for (Object ti : taxItemsList) {
                if (ti instanceof Map<?, ?> taxMap) {
                    FiscalBillTaxEntity tax = new FiscalBillTaxEntity();
                    tax.setFiscalbillId(entity.getFiscalbillId());
                    tax.setClientId(clientId);
                    tax.setOrgId(orgId);
                    tax.setEfiscalTaxlabel(str(taxMap.get("label")));
                    tax.setEfiscalCategoryname(str(taxMap.get("categoryName")));
                    if (taxMap.get("categoryType") != null) {
                        tax.setEfiscalCategorytype(((Number) taxMap.get("categoryType")).longValue());
                    }
                    if (taxMap.get("rate") != null) {
                        tax.setRate(new BigDecimal(str(taxMap.get("rate"))));
                    }
                    if (taxMap.get("amount") != null) {
                        tax.setAmount(new BigDecimal(str(taxMap.get("amount"))));
                    }
                    tax.setCreated(LocalDateTime.now());
                    tax.setUpdated(LocalDateTime.now());
                    fiscalBillTaxRepository.save(tax);
                }
            }
        }
    }

    private void savePaymentRecords(Long fiscalbillId, Long clientId, Long orgId,
            String paymentMethodCode, BigDecimal totalAmount) {
        if (totalAmount == null) return;
        int paymentType = resolvePaymentType(clientId, paymentMethodCode);
        FiscalBillPayEntity pay = new FiscalBillPayEntity();
        pay.setFiscalbillId(fiscalbillId);
        pay.setClientId(clientId);
        pay.setOrgId(orgId);
        pay.setPaymentType(paymentType);
        pay.setAmount(totalAmount);
        pay.setCreated(LocalDateTime.now());
        pay.setUpdated(LocalDateTime.now());
        fiscalBillPayRepository.save(pay);
    }

    private void saveManualPaymentRecords(Long fiscalbillId, Long clientId, Long orgId,
            List<PaymentRequest> payments) {
        if (payments == null) return;
        for (PaymentRequest p : payments) {
            FiscalBillPayEntity pay = new FiscalBillPayEntity();
            pay.setFiscalbillId(fiscalbillId);
            pay.setClientId(clientId);
            pay.setOrgId(orgId);
            pay.setPaymentType(p.paymentType());
            pay.setAmount(p.amount());
            pay.setCreated(LocalDateTime.now());
            pay.setUpdated(LocalDateTime.now());
            fiscalBillPayRepository.save(pay);
        }
    }

    private void saveLineItems(Long fiscalbillId, Long clientId, Long orgId,
            List<FiscalBillItemRequest> items) {
        if (items == null) return;
        for (FiscalBillItemRequest item : items) {
            FiscalBillLineEntity line = new FiscalBillLineEntity();
            line.setFiscalbillId(fiscalbillId);
            line.setClientId(clientId);
            line.setOrgId(orgId);
            line.setName(item.name());
            line.setQuantity(item.quantity());
            line.setUnitPrice(item.unitPrice());
            line.setTotalAmount(item.totalAmount());
            line.setTaxLabel(resolvePrimaryLabel(item));
            line.setGtin(item.gtin());
            line.setProductId(item.productId());
            line.setSku(item.sku());
            line.setCreated(LocalDateTime.now());
            line.setUpdated(LocalDateTime.now());
            fiscalBillLineRepository.save(line);
        }
    }

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    private FiscalBillEntity createPendingEntity(Long orgId, Long clientId,
            String orderId, int invoiceType, int transactionType, String customerName, String requestBody) {
        FiscalBillEntity e = new FiscalBillEntity();
        e.setOrgId(orgId);
        e.setClientId(clientId);
        e.setOrderId(orderId);
        e.setRequestBody(requestBody);
        e.setStatus(STATUS_PENDING);
        e.setAttemptCount(1);
        e.setEfiscalInvoicetype(invoiceType);
        e.setEfiscalTransactiontype(transactionType);
        e.setEfiscalCustomername(customerName);
        e.setIsactive("Y");
        e.setProcessed("N");
        LocalDateTime now = LocalDateTime.now();
        e.setCreated(now);
        e.setUpdated(now);
        return e;
    }

    private void registerIdempotencyKey(String idempotencyKey, FiscalBillEntity entity) {
        FiscalBillIdempotencyKeyEntity key = new FiscalBillIdempotencyKeyEntity();
        key.setIdempotencyKey(idempotencyKey);
        key.setFiscalBill(entity);
        key.setCreatedAt(java.time.OffsetDateTime.now());
        idempotencyKeyRepository.save(key);
    }

    private FiscalBillConfigEntity resolveConfig(Long orgId) {
        return fiscalBillConfigRepository.findFirstByOrgIdAndIsactive(orgId, "Y").orElse(null);
    }

    private String belgradeNow() {
        return ZonedDateTime.now(ZoneId.of("Europe/Belgrade"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String toJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to serialize request body: " + ex.getMessage());
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String trimTo(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private List<String> resolveItemLabels(FiscalBillItemRequest item) {
        if (item.labels() != null && !item.labels().isEmpty()) {
            return item.labels();
        }
        if (item.taxLabel() != null && !item.taxLabel().isBlank()) {
            return List.of(item.taxLabel());
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Missing tax label for line item: " + safeItemName(item, -1));
    }

    private List<FiscalBillItemRequest> resolveVatLabelsForOrderItems(List<FiscalBillItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Order has no line items to fiscalize");
        }
        List<FiscalBillItemRequest> resolved = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            FiscalBillItemRequest item = items.get(i);
            String itemName = safeItemName(item, i);

            if (item.taxValue() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Order line item '" + itemName + "' is missing product_tax_percent value");
            }

            if (item.taxCategoryName() == null || item.taxCategoryName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Order line item '" + itemName + "' is missing product_tax_name value");
            }

            String taxCategoryName = item.taxCategoryName().trim();
            List<TaxEntity> categoryTaxes = taxRepository.findActiveTaxesByCategoryName(taxCategoryName);
            TaxEntity matchedTax = categoryTaxes.stream()
                    .filter(t -> t.getRate() != null && t.getRate().compareTo(item.taxValue()) == 0)
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "No mapped tax found for order line item '" + itemName
                                    + "' (product_tax_name='" + taxCategoryName
                                    + "', product_tax_percent=" + item.taxValue() + ")"));
            String taxLabel = matchedTax.getLabel();
            if (taxLabel == null || taxLabel.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tax mapping is missing label for order line item '" + itemName
                            + "' (product_tax_name='" + taxCategoryName
                            + "', product_tax_percent=" + item.taxValue() + ")");
            }

            String resolvedTaxPrefix = item.taxPrefix();
            if (resolvedTaxPrefix == null || resolvedTaxPrefix.isBlank()) {
                resolvedTaxPrefix = String.format("%02d", item.taxValue().intValue());
            }

            resolved.add(new FiscalBillItemRequest(
                    item.name(),
                    item.quantity(),
                    item.unitPrice(),
                    item.totalAmount(),
                    taxLabel,
                    resolvedTaxPrefix,
                    item.gtin(),
                    item.productId(),
                    item.sku(),
                    item.taxValue(),
                    item.taxCategoryName(),
                    List.of(taxLabel)
            ));
        }
        return resolved;
    }

    private String resolvePrimaryLabel(FiscalBillItemRequest item) {
        if (item.labels() != null && !item.labels().isEmpty()) {
            return item.labels().get(0);
        }
        if (item.taxLabel() != null && !item.taxLabel().isBlank()) {
            return item.taxLabel();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Missing tax label for line item: " + safeItemName(item, -1));
    }

    private String safeItemName(FiscalBillItemRequest item, int index) {
        if (item != null && item.name() != null && !item.name().isBlank()) {
            return item.name();
        }
        return index >= 0 ? "line #" + (index + 1) : "unnamed line";
    }

    private OrderFiscalizeRequest buildSyntheticOrderFromManual(ManualFiscalBillRequest request) {
        return new OrderFiscalizeRequest(
                request.orderId(),
                request.customerName(),
                null, // no billing type
                null, // no billing VAT
                null, // no payment method code
                request.items()
        );
    }

    private FiscalBillView toView(FiscalBillEntity e) {
        return new FiscalBillView(
                e.getFiscalbillId(),
                e.getOrderId(),
                e.getStatus(),
                e.getProviderReference(),
                e.getEfiscalSdcInvoiceno(),
            e.getEfiscalLink(),
            e.getEfiscalQr(),
                e.getLastError(),
                e.getAttemptCount(),
                e.getCreated() != null ? e.getCreated().toString() : null,
                e.getUpdated() != null ? e.getUpdated().toString() : null
        );
    }

    private FiscalBillListView toListView(FiscalBillEntity e) {
        return new FiscalBillListView(
                e.getFiscalbillId(),
                e.getOrderId(),
                e.getStatus(),
                e.getEfiscalCustomername(),
                e.getEfiscalInvoicetype(),
                e.getEfiscalTransactiontype(),
                e.getEfiscalSdcInvoiceno(),
                e.getEfiscalSdcdatetime(),
                e.getEfiscalTotalamount(),
                e.getLastError(),
                e.getCreated() != null ? e.getCreated().toString() : null,
                e.getUpdated() != null ? e.getUpdated().toString() : null
        );
    }

    // -----------------------------------------------------------------------
    // Request / Response records
    // -----------------------------------------------------------------------

    /** An individual line item on a fiscal bill. */
    public record FiscalBillItemRequest(
            String name,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            String taxLabel,
            String taxPrefix,   // 2-digit code for advance name template (e.g. "20")
            String gtin,
            String productId,
            String sku,
            BigDecimal taxValue,
            String taxCategoryName,
            List<String> labels
    ) {}

    /** Payment row for manual fiscal bill creation. */
    public record PaymentRequest(
            int paymentType,   // 0=Other,1=Cash,2=Card,3=Check,4=Wire,5=Voucher,6=MobileMoney
            BigDecimal amount
    ) {}

    /** Request object for order-based fiscalization. */
    public record OrderFiscalizeRequest(
            String orderId,
            String customerName,
            String billingType,          // "company" or individual
            String billingCompanyVat,
            String paymentMethodCode,    // e.g. cash_delivery, wire
            List<FiscalBillItemRequest> items
    ) {}

    /** Request object for manual fiscal bill creation. */
    public record ManualFiscalBillRequest(
            String orderId,             // Optional — if set, applies order-based checks
            String customerName,
            int invoiceType,
            int transactionType,
            String buyerType,           // Optional buyer type prefix (e.g. "10")
            String buyerVat,            // Optional company VAT
            List<FiscalBillItemRequest> items,
            List<PaymentRequest> payments
    ) {}

    public record FiscalBillView(
            Long fiscalbillId,
            String orderId,
            String status,
            String providerReference,
            String sdcInvoiceNumber,
            String efiscalLink,
            String efiscalQr,
            String lastError,
            Integer attemptCount,
            String createdAt,
            String updatedAt
    ) {}

            public record FiscalBillListView(
                Long fiscalbillId,
                String orderId,
                String status,
                String customerName,
                Integer invoiceType,
                Integer transactionType,
                String sdcInvoiceNumber,
                String sdcDateTime,
                BigDecimal totalAmount,
                String lastError,
                String createdAt,
                String updatedAt
            ) {}

            public record FiscalBillTaxView(
                Long fiscalbilltaxId,
                String taxLabel,
                String categoryName,
                Long categoryType,
                BigDecimal rate,
                BigDecimal amount
            ) {}

            public record FiscalBillLineView(
                Long fiscalbilllineId,
                String name,
                BigDecimal quantity,
                BigDecimal unitPrice,
                BigDecimal totalAmount,
                String taxLabel,
                String gtin,
                String productId,
                String sku
            ) {}

            public record FiscalBillPayView(
                Long fiscalbillpayId,
                Integer paymentType,
                BigDecimal amount
            ) {}

            public record FiscalBillDetailsView(
                FiscalBillView fiscalBill,
                List<FiscalBillTaxView> taxItems,
                List<FiscalBillLineView> lineItems,
                List<FiscalBillPayView> payments
            ) {}

    public record FiscalBillCreateResult(FiscalBillView fiscalBill, boolean created, boolean alreadyExists, boolean failed) {
        public static FiscalBillCreateResult ofCreated(FiscalBillView fb) {
            return new FiscalBillCreateResult(fb, true, false, false);
        }
        public static FiscalBillCreateResult ofAlreadyExists(FiscalBillView fb) {
            return new FiscalBillCreateResult(fb, false, true, false);
        }
        public static FiscalBillCreateResult ofFailed(FiscalBillView fb) {
            return new FiscalBillCreateResult(fb, false, false, true);
        }
    }

    public record FiscalBillRetryResult(FiscalBillView fiscalBill, boolean retried, boolean notFound,
                                        boolean notRetryable, boolean idempotencyConflict) {
        public static FiscalBillRetryResult ofRetried(FiscalBillView fb) {
            return new FiscalBillRetryResult(fb, true, false, false, false);
        }
        public static FiscalBillRetryResult ofNotFound() {
            return new FiscalBillRetryResult(null, false, true, false, false);
        }
        public static FiscalBillRetryResult ofNotRetryable(FiscalBillView fb) {
            return new FiscalBillRetryResult(fb, false, false, true, false);
        }
        public static FiscalBillRetryResult ofIdempotencyConflict() {
            return new FiscalBillRetryResult(null, false, false, false, true);
        }
    }
}

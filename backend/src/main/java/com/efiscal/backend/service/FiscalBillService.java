package com.efiscal.backend.service;

import com.efiscal.backend.model.FiscalBillEntity;
import com.efiscal.backend.model.FiscalBillIdempotencyKeyEntity;
import com.efiscal.backend.model.FiscalBillLineEntity;
import com.efiscal.backend.model.FiscalBillPayEntity;
import com.efiscal.backend.model.FiscalBillTaxEntity;
import com.efiscal.backend.model.PayTypeMapEntity;
import com.efiscal.backend.model.ProductEntity;
import com.efiscal.backend.model.TaxEntity;
import com.efiscal.backend.repository.FiscalBillIdempotencyKeyRepository;
import com.efiscal.backend.repository.FiscalBillLineRepository;
import com.efiscal.backend.repository.FiscalBillPayRepository;
import com.efiscal.backend.repository.FiscalBillRepository;
import com.efiscal.backend.repository.FiscalBillTaxRepository;
import com.efiscal.backend.repository.OrgRepository;
import com.efiscal.backend.repository.PayTypeMapRepository;
import com.efiscal.backend.repository.ProductRepository;
import com.efiscal.backend.repository.TaxRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
 * - Header fields: invoiceType, transactionType, dateAndTime (Belgrade TZ), invoiceNumber from the ESIR number
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
    public static final int INVOICE_TYPE_PROFORMA = 1;
    public static final int INVOICE_TYPE_COPY = 2;
    public static final int INVOICE_TYPE_TRAINING = 3;
    public static final int INVOICE_TYPE_ADVANCE = 4;

    /** Transaction type constants */
    public static final int TRANSACTION_TYPE_SALE = 0;
    public static final int TRANSACTION_TYPE_REFUND = 1;

    /** How far in the past an advance payment moment may be set. */
    private static final int ADVANCE_PAYMENT_MAX_DAYS_IN_PAST = 3;

    private static final ZoneId BELGRADE_ZONE = ZoneId.of("Europe/Belgrade");

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
    private final PayTypeMapRepository payTypeMapRepository;
    private final ProductRepository productRepository;
    private final TaxRepository taxRepository;
    private final TaxAuthorityService taxAuthorityService;
    private final FiscalBillEmailService fiscalBillEmailService;
    private final EsirNumberService esirNumberService;
    private final ObjectMapper objectMapper;
    private final OrgRepository orgRepository;

    public FiscalBillService(
            FiscalBillRepository fiscalBillRepository,
            FiscalBillTaxRepository fiscalBillTaxRepository,
            FiscalBillPayRepository fiscalBillPayRepository,
            FiscalBillLineRepository fiscalBillLineRepository,
            FiscalBillIdempotencyKeyRepository idempotencyKeyRepository,
            PayTypeMapRepository payTypeMapRepository,
            ProductRepository productRepository,
            TaxRepository taxRepository,
            TaxAuthorityService taxAuthorityService,
            FiscalBillEmailService fiscalBillEmailService,
            EsirNumberService esirNumberService,
            ObjectMapper objectMapper,
            OrgRepository orgRepository) {
        this.fiscalBillRepository = fiscalBillRepository;
        this.fiscalBillTaxRepository = fiscalBillTaxRepository;
        this.fiscalBillPayRepository = fiscalBillPayRepository;
        this.fiscalBillLineRepository = fiscalBillLineRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.payTypeMapRepository = payTypeMapRepository;
        this.productRepository = productRepository;
        this.taxRepository = taxRepository;
        this.taxAuthorityService = taxAuthorityService;
        this.fiscalBillEmailService = fiscalBillEmailService;
        this.esirNumberService = esirNumberService;
        this.objectMapper = objectMapper;
        this.orgRepository = orgRepository;
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

        requireNoDuplicateOrderBill(orgId, orderId, invoiceType, transactionType);

        // Resolve order item tax labels before any dependent flow (including advance-refund chain).
        List<FiscalBillItemRequest> orderItems = appendShipmentLineIfApplicable(orgId, orderData);
        List<FiscalBillItemRequest> resolvedItems = applyAdvanceSaleTotalPaidDefaults(
                invoiceType, transactionType,
                enrichItemsWithGtin(orgId, resolveVatLabelsForOrderItems(orderItems)));

        // --- 4.1.5  Advance closing chain ---
        // If creating Normal Sale and Advance Sale exists → first create Advance Refund
        if (invoiceType == INVOICE_TYPE_NORMAL && transactionType == TRANSACTION_TYPE_SALE) {
            List<FiscalBillEntity> advanceBills = fiscalBillRepository
                    .findByOrgIdAndOrderIdAndInvoiceTypeAndTransactionType(
                            orgId, orderId, INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_SALE);
            if (!advanceBills.isEmpty()) {
                // Create Advance Refund to close the chain
            createAdvanceRefund(orgId, clientId, orderId, advanceBills, orderData, resolvedItems, null);
            }
        }

        // Build and send request
        BuiltTaxAuthorityRequest builtRequest = buildRequestBody(orgId, clientId, orderId, invoiceType, transactionType,
            resolvedItems, orderData.paymentMethodCode(), orderData.billingType(),
                orderData.billingCompanyVat(), orderData.cashier(), orderData.buyerCostCenterId(),
                orderData.dateAndTimeOfIssue());
        String requestBody = builtRequest.json();

        FiscalBillEntity entity = createPendingEntity(orgId, clientId, orderId,
            invoiceType, transactionType, orderData.customerName(), orderData.customerEmail(),
            builtRequest.buyerId(), builtRequest.buyerCostCenterId(), requestBody, builtRequest.referentFiscalbillId(),
            builtRequest.dateAndTimeOfIssue());
        fiscalBillRepository.save(entity);
        registerIdempotencyKey(idempotencyKey, entity);

        try {
            String response = taxAuthorityService.call(orgId, "CREATE_INVOICE", requestBody);
            processTaxAuthorityResponse(entity, response, invoiceType, transactionType, resolvedItems, clientId, orgId);
            fiscalBillRepository.save(entity);
            // Save payment records
            savePaymentRecords(entity.getFiscalbillId(), clientId, orgId, orderData.paymentMethodCode(),
                    entity.getEfiscalTotalamount());
            // Save line items after successful fiscalization (Advance: summarized tax lines)
            saveLineItems(entity.getFiscalbillId(), clientId, orgId, linesToPersist(resolvedItems));
            FiscalBillEmailService.EmailSendResult emailResult = fiscalBillEmailService.sendIfRequested(
                    orgId, entity, orderData.sendEmail(), orderData.customerEmail(), orderData.customerName(), orderId);
            return FiscalBillCreateResult.ofCreated(toView(entity, emailResult));
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

        validateManualRequestAmounts(
                request.items(), request.payments(), request.invoiceType(), request.transactionType());
        validateManualReferentChainRules(orgId, request);

        // If an orderId is provided, apply order-linked fiscal-chain checks (spec 4.2.1)
        String orderId = request.orderId();
        if (orderId != null && orderId.isBlank()) {
            orderId = null;
        }

        if (orderId != null) {
            applyManualOrderLinkedChecks(orgId, orderId, request);

            // 4.1.5 Advance closing chain (also applies when orderId is provided in manual creation)
            if (request.invoiceType() == INVOICE_TYPE_NORMAL && request.transactionType() == TRANSACTION_TYPE_SALE) {
                List<FiscalBillEntity> advanceBills = fiscalBillRepository
                        .findByOrgIdAndOrderIdAndInvoiceTypeAndTransactionType(
                                orgId, orderId, INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_SALE);
                if (!advanceBills.isEmpty()) {
                    OrderFiscalizeRequest syntheticOrder = buildSyntheticOrderFromManual(request);
                    createAdvanceRefund(orgId, clientId, orderId, advanceBills, syntheticOrder,
                            resolveItemsForFiscalChain(request.items()), request.payments());
                }
            }
        }

        List<FiscalBillItemRequest> enrichedItems = applyAdvanceSaleTotalPaidDefaults(
                request.invoiceType(), request.transactionType(),
                enrichItemsWithGtin(orgId, request.items()));

        // Build request body — manual items, manual payments
        BuiltTaxAuthorityRequest builtRequest = buildManualRequestBody(orgId, clientId, orderId,
                request.invoiceType(), request.transactionType(),
            enrichedItems, request.payments(),
                request.buyerId(), request.buyerType(), request.buyerVat(),
                request.buyerCostCenterId(),
                request.referentDocumentNumber(), request.cashier(),
                request.dateAndTimeOfIssue());
        String requestBody = builtRequest.json();

        FiscalBillEntity entity = createPendingEntity(orgId, clientId, orderId,
            request.invoiceType(), request.transactionType(), request.customerName(), request.customerEmail(),
            builtRequest.buyerId(), builtRequest.buyerCostCenterId(), requestBody, builtRequest.referentFiscalbillId(),
            builtRequest.dateAndTimeOfIssue());
        fiscalBillRepository.save(entity);
        registerIdempotencyKey(idempotencyKey, entity);

        try {
            String response = taxAuthorityService.call(orgId, "CREATE_INVOICE", requestBody);
            processTaxAuthorityResponse(entity, response, request.invoiceType(), request.transactionType(),
                enrichedItems, clientId, orgId);
            fiscalBillRepository.save(entity);
            // Save payment records from manual payment rows
            saveManualPaymentRecords(entity.getFiscalbillId(), clientId, orgId, request.payments());
            // Save line items (Advance: summarized tax lines with configured advance names)
            saveLineItems(entity.getFiscalbillId(), clientId, orgId, linesToPersist(enrichedItems));
            FiscalBillEmailService.EmailSendResult emailResult = fiscalBillEmailService.sendIfRequested(
                    orgId, entity, request.sendEmail(), request.customerEmail(), request.customerName(), orderId);
            return FiscalBillCreateResult.ofCreated(toView(entity, emailResult));
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
    public FiscalBillCreateResult createCopyFiscalBill(Long sourceFiscalBillId, String idempotencyKey, String cashier) {
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

        String requestBody = buildCopyRequestBody(source, copyItems, copyPayments, cashier);

        int copyTransactionType = source.getEfiscalTransactiontype() == null
                ? TRANSACTION_TYPE_SALE
                : source.getEfiscalTransactiontype();

        FiscalBillEntity entity = createPendingEntity(
                source.getOrgId(),
                source.getClientId(),
                source.getOrderId(),
                INVOICE_TYPE_COPY,
                copyTransactionType,
                source.getCustomerName(),
                source.getCustomerEmail(),
                source.getCustomerId(),
                source.getCustomerCostCenterId(),
                requestBody,
                source.getFiscalbillId(),
                null); // Copy never sends dateAndTimeOfIssue
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
    public FiscalBillCreateResult createRefundFiscalBill(Long sourceFiscalBillId, String idempotencyKey, String cashier) {
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

        String requestBody = buildRefundRequestBody(source, refundItems, refundPayments, cashier);

        int refundInvoiceType = source.getEfiscalInvoicetype() == null
                ? INVOICE_TYPE_NORMAL
                : source.getEfiscalInvoicetype();

        FiscalBillEntity entity = createPendingEntity(
                source.getOrgId(),
                source.getClientId(),
                source.getOrderId(),
                refundInvoiceType,
                TRANSACTION_TYPE_REFUND,
                source.getCustomerName(),
                source.getCustomerEmail(),
                source.getCustomerId(),
                source.getCustomerCostCenterId(),
                requestBody,
                source.getFiscalbillId(),
                null); // Refund never sends dateAndTimeOfIssue
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
    private BuiltTaxAuthorityRequest buildRequestBody(Long orgId, Long clientId, String orderId,
            int invoiceType, int transactionType,
            List<FiscalBillItemRequest> items, String paymentMethodCode,
            String billingType, String billingCompanyVat,
            String cashier, String buyerCostCenterId, String dateAndTimeOfIssue) {

        Map<String, Object> body = new HashMap<>();

        // Header (4.1.2)
        body.put("invoiceType", invoiceType);
        body.put("transactionType", transactionType);
        String sentDateAndTimeOfIssue =
                putDateAndTimeOfIssueIfAllowed(body, invoiceType, transactionType, dateAndTimeOfIssue);
        putInvoiceNumberIfPresent(body);
        putCashierIfPresent(body, cashier);

        // BuyerId (4.1.7)
        String buyerId = resolveBuyerIdFromOrder(billingType, billingCompanyVat);
        if (buyerId != null) body.put("buyerId", buyerId);

        // Optional customer field — only when buyerId is present and value provided
        String resolvedCostCenterId = resolveBuyerCostCenterId(buyerCostCenterId, buyerId);
        if (resolvedCostCenterId != null) {
            body.put("buyerCostCenterId", resolvedCostCenterId);
        }

        // Reference fields (4.1.4)
        Long referentFiscalbillId = setReferentFields(body, orgId, orderId, invoiceType, transactionType);

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

        return new BuiltTaxAuthorityRequest(toJson(body), referentFiscalbillId, buyerId, resolvedCostCenterId,
                sentDateAndTimeOfIssue);
    }

    /**
     * Build Tax Authority request body for manual fiscalization.
     */
    private BuiltTaxAuthorityRequest buildManualRequestBody(Long orgId, Long clientId, String orderId,
            int invoiceType, int transactionType,
            List<FiscalBillItemRequest> items, List<PaymentRequest> payments,
            String buyerId, String buyerType, String buyerVat, String buyerCostCenterId,
            String referentDocumentNumber, String cashier, String dateAndTimeOfIssue) {

        Map<String, Object> body = new HashMap<>();

        // Header (4.1.2 / 4.2.1)
        body.put("invoiceType", invoiceType);
        body.put("transactionType", transactionType);
        String sentDateAndTimeOfIssue =
                putDateAndTimeOfIssueIfAllowed(body, invoiceType, transactionType, dateAndTimeOfIssue);
        putInvoiceNumberIfPresent(body);
        putCashierIfPresent(body, cashier);

        // BuyerId (4.2.1)
        String resolvedBuyerId = resolveManualBuyerId(buyerId, buyerType, buyerVat);
        if (resolvedBuyerId != null) {
            body.put("buyerId", resolvedBuyerId);
        }

        String resolvedCostCenterId = resolveBuyerCostCenterId(buyerCostCenterId, resolvedBuyerId);
        if (resolvedCostCenterId != null) {
            body.put("buyerCostCenterId", resolvedCostCenterId);
        }

        // Reference fields: explicit override resolves DT from local DB; else auto-resolve when orderId set.
        Long referentFiscalbillId = null;
        if (referentDocumentNumber != null && !referentDocumentNumber.isBlank()) {
            referentFiscalbillId = applyManualReferentFields(body, orgId, referentDocumentNumber.trim());
        } else if (orderId != null && !orderId.isBlank()) {
            referentFiscalbillId = setReferentFields(body, orgId, orderId, invoiceType, transactionType);
        }

        // Payment from manually entered payment rows (4.2.3)
        body.put("payment", buildPaymentArrayFromRows(payments));

        // Items (4.1.3)
        if (invoiceType == INVOICE_TYPE_ADVANCE) {
            body.put("items", buildAdvanceLineItems(items));
        } else {
            body.put("items", buildLineItems(items));
        }

        return new BuiltTaxAuthorityRequest(toJson(body), referentFiscalbillId, resolvedBuyerId, resolvedCostCenterId,
                sentDateAndTimeOfIssue);
    }

    private String resolveManualBuyerId(String buyerId, String buyerType, String buyerVat) {
        if (buyerId != null && !buyerId.isBlank()) {
            return buyerId.trim();
        }
        if (buyerType != null && buyerVat != null && !buyerVat.isBlank()) {
            return buyerType + ":" + buyerVat;
        }
        return null;
    }

    private String resolveBuyerCostCenterId(String buyerCostCenterId, String buyerId) {
        if (buyerId == null || buyerId.isBlank()) {
            return null;
        }
        if (buyerCostCenterId == null || buyerCostCenterId.isBlank()) {
            return null;
        }
        return buyerCostCenterId.trim();
    }

    private String buildCopyRequestBody(
            FiscalBillEntity source,
            List<FiscalBillItemRequest> items,
            List<PaymentRequest> payments,
            String cashier) {

        Map<String, Object> body = new HashMap<>();
        int sourceTransactionType = source.getEfiscalTransactiontype() == null
                ? TRANSACTION_TYPE_SALE
                : source.getEfiscalTransactiontype();

        body.put("invoiceType", INVOICE_TYPE_COPY);
        body.put("transactionType", sourceTransactionType);
        putInvoiceNumberIfPresent(body);
        putCashierIfPresent(body, cashier);

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
            String cashier) {

        Map<String, Object> body = new HashMap<>();
        int refundInvoiceType = source.getEfiscalInvoicetype() == null
                ? INVOICE_TYPE_NORMAL
                : source.getEfiscalInvoicetype();

        body.put("invoiceType", refundInvoiceType);
        body.put("transactionType", TRANSACTION_TYPE_REFUND);
        putInvoiceNumberIfPresent(body);
        putCashierIfPresent(body, cashier);

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
     * Tax Authority {@code dateAndTimeOfIssue} — sent only on Advance Sale bills for which the caller supplied
     * an advance payment moment. Every other invoice/transaction type must omit it, and an Advance Sale without
     * a chosen moment omits it too, so the Tax Authority stamps the bill itself.
     *
     * @return the value put in the request body, or null when the field is omitted
     */
    private String putDateAndTimeOfIssueIfAllowed(Map<String, Object> body,
            int invoiceType, int transactionType, String dateAndTimeOfIssue) {
        if (dateAndTimeOfIssue == null || dateAndTimeOfIssue.isBlank()) {
            return null;
        }
        if (invoiceType != INVOICE_TYPE_ADVANCE || transactionType != TRANSACTION_TYPE_SALE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateAndTimeOfIssue is allowed only for Advance Sale fiscal bills");
        }
        String normalized = normalizeAdvancePaymentDateTime(dateAndTimeOfIssue.trim());
        body.put("dateAndTimeOfIssue", normalized);
        return normalized;
    }

    /** Validates the advance payment moment is in the past, at most {@code ADVANCE_PAYMENT_MAX_DAYS_IN_PAST} days back. */
    private String normalizeAdvancePaymentDateTime(String value) {
        ZonedDateTime selected = parseBelgradeDateTime(value);
        ZonedDateTime now = ZonedDateTime.now(BELGRADE_ZONE);
        if (selected.isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateAndTimeOfIssue must not be in the future");
        }
        if (selected.isBefore(now.minusDays(ADVANCE_PAYMENT_MAX_DAYS_IN_PAST))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateAndTimeOfIssue must not be more than "
                            + ADVANCE_PAYMENT_MAX_DAYS_IN_PAST + " days in the past");
        }
        return selected.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private ZonedDateTime parseBelgradeDateTime(String value) {
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(BELGRADE_ZONE);
        } catch (DateTimeParseException withoutOffset) {
            try {
                return LocalDateTime.parse(value).atZone(BELGRADE_ZONE);
            } catch (DateTimeParseException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid dateAndTimeOfIssue: " + value);
            }
        }
    }

    /** Tax Authority {@code invoiceNumber} — ESIR number of this installation, as printed on receipts. */
    private void putInvoiceNumberIfPresent(Map<String, Object> body) {
        String esirNumber = esirNumberService.resolveEsirNumber();
        if (!esirNumber.isBlank()) {
            body.put("invoiceNumber", esirNumber);
        }
    }

    private void putCashierIfPresent(Map<String, Object> body, String cashier) {
        if (cashier != null && !cashier.isBlank()) {
            body.put("cashier", cashier.trim());
        }
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
     * Name format: {@code advancePrefix advanceName (taxMark)} from tax table.
     */
    private List<Map<String, Object>> buildAdvanceLineItems(List<FiscalBillItemRequest> items) {
        return toTaxAuthorityItems(buildAdvanceItemRequests(items));
    }

    /**
     * Group source items by tax label into Advance line requests (qty=1, name from tax config).
     */
    private List<FiscalBillItemRequest> buildAdvanceItemRequests(List<FiscalBillItemRequest> items) {
        Map<String, BigDecimal> groupedByLabel = new HashMap<>();
        for (FiscalBillItemRequest item : items) {
            String label = resolvePrimaryLabel(item);
            groupedByLabel.merge(label, effectiveLineAmount(item), BigDecimal::add);
        }
        if (groupedByLabel.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No line items for advance invoice");
        }

        Map<String, String> advanceNameByLabel = resolveAdvanceNameByLabel(groupedByLabel.keySet());

        List<FiscalBillItemRequest> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : groupedByLabel.entrySet()) {
            String label = entry.getKey();
            BigDecimal total = entry.getValue();
            result.add(new FiscalBillItemRequest(
                    advanceNameByLabel.get(label),
                    BigDecimal.ONE,
                    total,
                    total,
                    label,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(label),
                    null
            ));
        }
        return result;
    }

    private List<Map<String, Object>> toTaxAuthorityItems(List<FiscalBillItemRequest> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (FiscalBillItemRequest item : items) {
            Map<String, Object> line = new HashMap<>();
            line.put("name", item.name());
            line.put("quantity", item.quantity());
            line.put("unitPrice", item.unitPrice());
            line.put("totalAmount", item.totalAmount());
            line.put("labels", item.labels() != null ? item.labels() : List.of(resolvePrimaryLabel(item)));
            result.add(line);
        }
        return result;
    }

    /**
     * Lines stored on {@code fiscalbillline} after a successful Tax Authority call.
     * Advance Sale keeps the original product lines so the PDF advertisement can list
     * name + price; the main items section still renders tax-grouped advance names via
     * {@code FiscalBillPdfService}. Advance Refund call sites pass summarized lines directly.
     */
    private List<FiscalBillItemRequest> linesToPersist(List<FiscalBillItemRequest> items) {
        return items;
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

            resolved.put(label, AdvanceLineNameResolver.format(
                    tax.getEfiscalAdvanceprefix(),
                    tax.getEfiscalAdvancename(),
                    label));
        }
        return resolved;
    }

    /**
     * Set referentDocumentNumber and referentDocumentDT fields (4.1.4).
     *
     * @return local fiscalbill_id of the referenced bill, or null when no referent applies
     */
    private Long setReferentFields(Map<String, Object> body, Long orgId, String orderId,
            int invoiceType, int transactionType) {
        FiscalBillEntity ref = null;

        if (invoiceType == INVOICE_TYPE_NORMAL && transactionType == TRANSACTION_TYPE_SALE) {
            // Normal Sale references to last issued Advance Refund (closes advance chain)
            ref = fiscalBillRepository
                    .findLatestByOrgAndOrderAndType(orgId, orderId, INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_REFUND)
                    .orElse(null);
        } else if (invoiceType == INVOICE_TYPE_NORMAL && transactionType == TRANSACTION_TYPE_REFUND) {
            // Normal Refund references to Normal Sale
            ref = fiscalBillRepository
                    .findLatestByOrgAndOrderAndType(orgId, orderId, INVOICE_TYPE_NORMAL, TRANSACTION_TYPE_SALE)
                    .orElse(null);
        } else if (invoiceType == INVOICE_TYPE_ADVANCE && transactionType == TRANSACTION_TYPE_SALE) {
            // Advance Sale can reference to last Advance Sale (for chained advances)
            ref = fiscalBillRepository
                    .findLatestByOrgAndOrderAndType(orgId, orderId, INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_SALE)
                    .orElse(null);
        }

        if (ref != null && ref.getEfiscalSdcInvoiceno() != null) {
            body.put("referentDocumentNumber", ref.getEfiscalSdcInvoiceno());
            if (ref.getEfiscalSdcdatetime() != null) {
                body.put("referentDocumentDT", ref.getEfiscalSdcdatetime());
            }
            return ref.getFiscalbillId();
        }
        return null;
    }

    /**
     * Apply user-supplied referent document number and resolve datetime from local fiscal bill (4.2.1 / 4.1.4).
     *
     * @return local fiscalbill_id of the referenced bill
     */
    private Long applyManualReferentFields(Map<String, Object> body, Long orgId, String referentDocumentNumber) {
        FiscalBillEntity ref = fiscalBillRepository
                .findFirstByOrgIdAndEfiscalSdcInvoicenoOrderByCreatedDesc(orgId, referentDocumentNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Referenced fiscal bill not found for number: " + referentDocumentNumber));
        if (ref.getEfiscalSdcdatetime() == null || ref.getEfiscalSdcdatetime().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Referenced fiscal bill is missing Tax Authority datetime for number: " + referentDocumentNumber);
        }
        body.put("referentDocumentNumber", referentDocumentNumber);
        body.put("referentDocumentDT", ref.getEfiscalSdcdatetime());
        return ref.getFiscalbillId();
    }

    private static final String CLOSE_ADVANCE_REFUND_REQUIRED_MESSAGE =
            "Referentni broj zatvaranja avansa nije Avans Refundacija, molimo vas proverite";

    /**
     * Manual referent-chain rules for Advance Sale partial pays, Close Advance (Normal Sale),
     * and invoice/transaction type pairing when a referent is supplied outside Close Advance.
     */
    private void validateManualReferentChainRules(Long orgId, ManualFiscalBillRequest request) {
        String referentDocumentNumber = request.referentDocumentNumber();
        if (referentDocumentNumber == null || referentDocumentNumber.isBlank()) {
            return;
        }
        String refNo = referentDocumentNumber.trim();
        // Close Advance (Zatvaranje avansa): Normal Sale with referent → Advance Refund + product chain
        if (request.invoiceType() == INVOICE_TYPE_NORMAL && request.transactionType() == TRANSACTION_TYPE_SALE) {
            validateCloseAdvanceReferentChain(orgId, refNo, request.items());
            return;
        }
        // Chained Advance Sale: referent must be Advance Sale (+ totalPaid / product rules)
        if (request.invoiceType() == INVOICE_TYPE_ADVANCE && request.transactionType() == TRANSACTION_TYPE_SALE) {
            validateAdvanceSaleReferentChain(orgId, refNo, request.items());
            return;
        }
        // All other manual bills with a referent: enforce allowed source invoice/transaction types
        validateReferentTypePairing(orgId, refNo, request.invoiceType(), request.transactionType());
    }

    /**
     * When Close Advance is not used, a supplied referent must match the creating bill's type pair:
     * Normal/Training/Advance/Proforma Refund → matching Sale;
     * Copy Refund → Normal or Advance Refund; Copy Sale → Normal or Advance Sale.
     */
    private void validateReferentTypePairing(
            Long orgId, String referentDocumentNumber, int invoiceType, int transactionType) {
        FiscalBillEntity ref = requireReferentBill(orgId, referentDocumentNumber);
        String message = null;
        if (invoiceType == INVOICE_TYPE_NORMAL && transactionType == TRANSACTION_TYPE_REFUND) {
            if (!isBillOfType(ref, INVOICE_TYPE_NORMAL, TRANSACTION_TYPE_SALE)) {
                message = "Referentni dokument za Normalnu Refundaciju mora biti Normalna Prodaja";
            }
        } else if (invoiceType == INVOICE_TYPE_TRAINING && transactionType == TRANSACTION_TYPE_REFUND) {
            if (!isBillOfType(ref, INVOICE_TYPE_TRAINING, TRANSACTION_TYPE_SALE)) {
                message = "Referentni dokument za Trening Refundaciju mora biti Trening Prodaja";
            }
        } else if (invoiceType == INVOICE_TYPE_ADVANCE && transactionType == TRANSACTION_TYPE_REFUND) {
            if (!isBillOfType(ref, INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_SALE)) {
                message = "Referentni dokument za Avans Refundaciju mora biti Avans Prodaja";
            }
        } else if (invoiceType == INVOICE_TYPE_PROFORMA && transactionType == TRANSACTION_TYPE_REFUND) {
            if (!isBillOfType(ref, INVOICE_TYPE_PROFORMA, TRANSACTION_TYPE_SALE)) {
                message = "Referentni dokument za Predračun Refundaciju mora biti Predračun Prodaja";
            }
        } else if (invoiceType == INVOICE_TYPE_COPY && transactionType == TRANSACTION_TYPE_REFUND) {
            if (!isBillOfType(ref, INVOICE_TYPE_NORMAL, TRANSACTION_TYPE_REFUND)
                    && !isBillOfType(ref, INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_REFUND)) {
                message = "Referentni dokument za Kopiju Refundacije mora biti Normalna ili Avans Refundacija";
            }
        } else if (invoiceType == INVOICE_TYPE_COPY && transactionType == TRANSACTION_TYPE_SALE) {
            if (!isBillOfType(ref, INVOICE_TYPE_NORMAL, TRANSACTION_TYPE_SALE)
                    && !isBillOfType(ref, INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_SALE)) {
                message = "Referentni dokument za Kopiju Prodaje mora biti Normalna ili Avans Prodaja";
            }
        }
        if (message != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private static boolean isBillOfType(FiscalBillEntity bill, int invoiceType, int transactionType) {
        return bill.getEfiscalInvoicetype() != null && bill.getEfiscalInvoicetype() == invoiceType
                && bill.getEfiscalTransactiontype() != null && bill.getEfiscalTransactiontype() == transactionType;
    }

    private void validateAdvanceSaleReferentChain(
            Long orgId, String referentDocumentNumber, List<FiscalBillItemRequest> submittingItems) {
        FiscalBillEntity firstRef = requireReferentBill(orgId, referentDocumentNumber);
        if (!isAdvanceSaleBill(firstRef)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Referenced fiscal bill must be an Advance Sale for chained advances");
        }
        List<FiscalBillEntity> chain = collectReferentChain(firstRef);
        for (FiscalBillEntity bill : chain) {
            if (!isAdvanceSaleBill(bill)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Referent chain must contain only Advance Sale bills");
            }
            List<FiscalBillLineEntity> lines = fiscalBillLineRepository.findByFiscalbillId(bill.getFiscalbillId());
            assertMatchingProducts(submittingItems, lines, bill.getEfiscalSdcInvoiceno());
        }
        for (FiscalBillItemRequest item : submittingItems) {
            String key = productMatchKey(item);
            BigDecimal lineCap = item.totalAmount() != null ? item.totalAmount() : BigDecimal.ZERO;
            BigDecimal paidSum = item.totalPaid() != null ? item.totalPaid() : BigDecimal.ZERO;
            for (FiscalBillEntity bill : chain) {
                FiscalBillLineEntity matched = findMatchingLine(
                        fiscalBillLineRepository.findByFiscalbillId(bill.getFiscalbillId()), key);
                if (matched == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Missing matching product in referent chain for: " + key);
                }
                if (matched.getTotalAmount() != null
                        && matched.getTotalAmount().subtract(lineCap).abs().compareTo(PAYMENT_TOTAL_TOLERANCE) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Line totalAmount does not match referent chain for product: " + key);
                }
                paidSum = paidSum.add(matched.getTotalPaid() != null ? matched.getTotalPaid() : BigDecimal.ZERO);
            }
            if (paidSum.compareTo(lineCap) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sum of totalPaid across advance chain exceeds totalAmount for product: " + key);
            }
        }
    }

    private void validateCloseAdvanceReferentChain(
            Long orgId, String referentDocumentNumber, List<FiscalBillItemRequest> submittingItems) {
        FiscalBillEntity firstRef = requireReferentBill(orgId, referentDocumentNumber);
        if (!isAdvanceRefundBill(firstRef)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CLOSE_ADVANCE_REFUND_REQUIRED_MESSAGE);
        }
        List<FiscalBillEntity> chain = collectReferentChain(firstRef);
        List<FiscalBillEntity> advanceSales = chain.stream()
                .filter(this::isAdvanceSaleBill)
                .toList();
        if (advanceSales.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Close Advance referent chain has no Advance Sale bills");
        }
        for (FiscalBillEntity advance : advanceSales) {
            List<FiscalBillLineEntity> lines = fiscalBillLineRepository.findByFiscalbillId(advance.getFiscalbillId());
            assertMatchingProducts(submittingItems, lines, advance.getEfiscalSdcInvoiceno());
            for (FiscalBillItemRequest item : submittingItems) {
                String key = productMatchKey(item);
                FiscalBillLineEntity matched = findMatchingLine(lines, key);
                if (matched == null || matched.getTotalAmount() == null || item.totalAmount() == null
                        || matched.getTotalAmount().subtract(item.totalAmount()).abs()
                                .compareTo(PAYMENT_TOTAL_TOLERANCE) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Advance Sale line totalAmount does not match Normal Sale for product: " + key);
                }
            }
        }
    }

    private FiscalBillEntity requireReferentBill(Long orgId, String referentDocumentNumber) {
        return fiscalBillRepository
                .findFirstByOrgIdAndEfiscalSdcInvoicenoOrderByCreatedDesc(orgId, referentDocumentNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Referenced fiscal bill not found for number: " + referentDocumentNumber));
    }

    /** First bill + all ancestors via referent_fiscalbill_id (cycle-safe). */
    private List<FiscalBillEntity> collectReferentChain(FiscalBillEntity first) {
        List<FiscalBillEntity> chain = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        FiscalBillEntity current = first;
        while (current != null) {
            Long id = current.getFiscalbillId();
            if (id == null || !seen.add(id)) {
                break;
            }
            chain.add(current);
            Long parentId = current.getReferentFiscalbillId();
            if (parentId == null) {
                break;
            }
            current = fiscalBillRepository.findById(parentId).orElse(null);
        }
        return chain;
    }

    private boolean isAdvanceSaleBill(FiscalBillEntity bill) {
        return bill.getEfiscalInvoicetype() != null && bill.getEfiscalInvoicetype() == INVOICE_TYPE_ADVANCE
                && bill.getEfiscalTransactiontype() != null && bill.getEfiscalTransactiontype() == TRANSACTION_TYPE_SALE;
    }

    private boolean isAdvanceRefundBill(FiscalBillEntity bill) {
        return bill.getEfiscalInvoicetype() != null && bill.getEfiscalInvoicetype() == INVOICE_TYPE_ADVANCE
                && bill.getEfiscalTransactiontype() != null && bill.getEfiscalTransactiontype() == TRANSACTION_TYPE_REFUND;
    }

    private void assertMatchingProducts(
            List<FiscalBillItemRequest> submittingItems,
            List<FiscalBillLineEntity> referentLines,
            String referentInvoiceNo) {
        if (submittingItems == null || referentLines == null
                || submittingItems.size() != referentLines.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Referent bill line count does not match (" + referentInvoiceNo + ")");
        }
        Set<String> submittingKeys = new HashSet<>();
        for (FiscalBillItemRequest item : submittingItems) {
            String key = productMatchKey(item);
            if (!submittingKeys.add(key)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate product key in submitting items: " + key);
            }
            if (findMatchingLine(referentLines, key) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product not found on referent bill " + referentInvoiceNo + ": " + key);
            }
        }
    }

    private FiscalBillLineEntity findMatchingLine(List<FiscalBillLineEntity> lines, String key) {
        if (lines == null || key == null) {
            return null;
        }
        for (FiscalBillLineEntity line : lines) {
            if (key.equals(productMatchKey(line))) {
                return line;
            }
        }
        return null;
    }

    private static String productMatchKey(FiscalBillItemRequest item) {
        return productMatchKey(item.productId(), item.sku(), item.gtin(), item.name());
    }

    private static String productMatchKey(FiscalBillLineEntity line) {
        return productMatchKey(line.getProductId(), line.getSku(), line.getGtin(), line.getName());
    }

    private static String productMatchKey(String productId, String sku, String gtinOrEan, String name) {
        String product = trimToNullStatic(productId);
        if (product != null) {
            return "id:" + product;
        }
        String skuKey = trimToNullStatic(sku);
        if (skuKey != null) {
            return "sku:" + skuKey;
        }
        String gtin = trimToNullStatic(gtinOrEan);
        if (gtin != null) {
            return "gtin:" + gtin;
        }
        String n = trimToNullStatic(name);
        return "name:" + (n == null ? "" : n.toLowerCase());
    }

    private static String trimToNullStatic(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Order-linked prechecks for manual creation when orderId is provided (spec 4.2.1).
     * Enforces duplicate protection scoped to organization; does not fetch MerchantPro order data.
     */
    private void applyManualOrderLinkedChecks(Long orgId, String orderId, ManualFiscalBillRequest request) {
        requireNoDuplicateOrderBill(orgId, orderId, request.invoiceType(), request.transactionType());
    }

    /**
     * Rejects a second successful bill for the same order + invoiceType + transactionType (org-scoped).
     * Training bills (invoiceType 3) are exempt: they may be issued any number of times per order.
     */
    private void requireNoDuplicateOrderBill(Long orgId, String orderId, int invoiceType, int transactionType) {
        if (invoiceType == INVOICE_TYPE_TRAINING) {
            return;
        }
        Optional<FiscalBillEntity> duplicate = fiscalBillRepository
                .findLatestByOrgAndOrderAndType(orgId, orderId, invoiceType, transactionType);
        if (duplicate.isPresent() && STATUS_SUCCESS.equals(duplicate.get().getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Fiscal bill already exists for order " + orderId +
                    " with invoiceType=" + invoiceType + " transactionType=" + transactionType);
        }
    }

    private static final BigDecimal PAYMENT_TOTAL_TOLERANCE = new BigDecimal("0.01");

    /**
     * Validate manual payment rows against line item totals (spec 4.2.3).
     * Advance Sale uses totalPaid (effective amount) for the bill total.
     */
    private void validateManualRequestAmounts(
            List<FiscalBillItemRequest> items,
            List<PaymentRequest> payments,
            int invoiceType,
            int transactionType) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No line items provided");
        }
        if (payments == null || payments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one payment is required");
        }

        boolean advanceSale = invoiceType == INVOICE_TYPE_ADVANCE && transactionType == TRANSACTION_TYPE_SALE;
        BigDecimal itemsTotal = BigDecimal.ZERO;
        for (FiscalBillItemRequest item : items) {
            if (item.quantity() == null || item.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each line item must have a positive quantity");
            }
            if (item.unitPrice() == null || item.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each line item must have a non-negative unit price");
            }
            if (item.totalAmount() == null || item.totalAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each line item must have a non-negative total amount");
            }
            if (advanceSale) {
                if (item.totalPaid() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Each Advance Sale line item must have totalPaid");
                }
                if (item.totalPaid().compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Each line item totalPaid must be non-negative");
                }
                if (item.totalPaid().compareTo(item.totalAmount()) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "totalPaid must not exceed totalAmount for line item: " + safeItemName(item, -1));
                }
            }
            itemsTotal = itemsTotal.add(advanceSale ? item.totalPaid() : item.totalAmount());
        }

        BigDecimal paymentsTotal = BigDecimal.ZERO;
        for (PaymentRequest payment : payments) {
            if (payment.amount() == null || payment.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each payment must have a positive amount");
            }
            paymentsTotal = paymentsTotal.add(payment.amount());
        }

        if (itemsTotal.subtract(paymentsTotal).abs().compareTo(PAYMENT_TOTAL_TOLERANCE) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment total does not match fiscal bill total");
        }
    }

    /** Amount used for Advance Sale TA grouping / bill total: totalPaid when set, else totalAmount. */
    private static BigDecimal effectiveLineAmount(FiscalBillItemRequest item) {
        if (item.totalPaid() != null) {
            return item.totalPaid();
        }
        return item.totalAmount() != null ? item.totalAmount() : BigDecimal.ZERO;
    }

    /**
     * For Advance Sale, ensure every line has totalPaid (defaulting null to totalAmount).
     */
    private List<FiscalBillItemRequest> applyAdvanceSaleTotalPaidDefaults(
            int invoiceType, int transactionType, List<FiscalBillItemRequest> items) {
        if (invoiceType != INVOICE_TYPE_ADVANCE || transactionType != TRANSACTION_TYPE_SALE
                || items == null || items.isEmpty()) {
            return items;
        }
        List<FiscalBillItemRequest> result = new ArrayList<>();
        for (FiscalBillItemRequest item : items) {
            if (item.totalPaid() != null) {
                result.add(item);
            } else {
                result.add(new FiscalBillItemRequest(
                        item.name(),
                        item.quantity(),
                        item.unitPrice(),
                        item.totalAmount(),
                        item.taxLabel(),
                        item.taxPrefix(),
                        item.gtin(),
                        item.productId(),
                        item.sku(),
                        item.taxValue(),
                        item.taxCategoryName(),
                        item.labels(),
                        item.totalAmount()));
            }
        }
        return result;
    }

    /**
     * Resolve line items for advance-close and similar fiscal-chain flows.
     * Manual items use taxLabel/labels; order-normalized items use taxValue + taxCategoryName.
     */
    private List<FiscalBillItemRequest> resolveItemsForFiscalChain(List<FiscalBillItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No line items provided");
        }

        boolean allManualLabels = items.stream().allMatch(item ->
                (item.labels() != null && !item.labels().isEmpty())
                        || (item.taxLabel() != null && !item.taxLabel().isBlank()));
        if (allManualLabels) {
            return items;
        }

        boolean allOrderFields = items.stream().allMatch(item ->
                item.taxValue() != null
                        && item.taxCategoryName() != null && !item.taxCategoryName().isBlank());
        if (allOrderFields) {
            return resolveVatLabelsForOrderItems(items);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Line items must have tax labels or order tax fields (product_tax_name and product_tax_percent)");
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
        if (billingType == null || !"company".equalsIgnoreCase(billingType.trim())) {
            return null;
        }
        if (billingCompanyVat == null || billingCompanyVat.isBlank()) {
            return null;
        }
        String vat = billingCompanyVat.trim().replaceAll("\\s+", "");
        if (vat.regionMatches(true, 0, "10:", 0, 3)) {
            vat = vat.substring(3);
        }
        if (vat.isBlank()) {
            return null;
        }
        return "10:" + vat;
    }

    // -----------------------------------------------------------------------
    // 4.1.5  Advance closing chain
    // -----------------------------------------------------------------------

    private void createAdvanceRefund(Long orgId, Long clientId, String orderId,
            List<FiscalBillEntity> advanceBills, OrderFiscalizeRequest orderData,
            List<FiscalBillItemRequest> resolvedItems, List<PaymentRequest> sourcePayments) {
        // Summarize all previous Advance Normal amounts
        BigDecimal totalAdvanceAmount = advanceBills.stream()
                .filter(b -> STATUS_SUCCESS.equals(b.getStatus()))
                .map(b -> b.getEfiscalTotalamount() != null ? b.getEfiscalTotalamount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAdvanceAmount.compareTo(BigDecimal.ZERO) == 0) return;

        // Get the last advance bill as reference
        FiscalBillEntity lastAdvance = advanceBills.get(0);

        List<FiscalBillItemRequest> refundSourceItems = collectAdvanceSaleItems(advanceBills, resolvedItems);
        List<FiscalBillItemRequest> advanceLines = buildAdvanceItemRequests(refundSourceItems);
        List<PaymentRequest> refundPayments = buildAdvanceRefundPayments(
                clientId, orderData.paymentMethodCode(), sourcePayments, lastAdvance, totalAdvanceAmount);

        Map<String, Object> body = new HashMap<>();
        body.put("invoiceType", INVOICE_TYPE_ADVANCE);
        body.put("transactionType", TRANSACTION_TYPE_REFUND);
        putInvoiceNumberIfPresent(body);
        // Reference the last advance bill
        if (lastAdvance.getEfiscalSdcInvoiceno() != null) {
            body.put("referentDocumentNumber", lastAdvance.getEfiscalSdcInvoiceno());
            if (lastAdvance.getEfiscalSdcdatetime() != null) {
                body.put("referentDocumentDT", lastAdvance.getEfiscalSdcdatetime());
            }
        }
        String buyerId = resolveBuyerIdFromOrder(orderData.billingType(), orderData.billingCompanyVat());
        if (buyerId != null) {
            body.put("buyerId", buyerId);
        }
        String resolvedCostCenterId = resolveBuyerCostCenterId(orderData.buyerCostCenterId(), buyerId);
        if (resolvedCostCenterId != null) {
            body.put("buyerCostCenterId", resolvedCostCenterId);
        }
        putCashierIfPresent(body, orderData.cashier());
        body.put("payment", buildPaymentArrayFromRows(refundPayments));
        body.put("items", toTaxAuthorityItems(advanceLines));

        String advanceRefundRequestBody = toJson(body);

        FiscalBillEntity refundEntity = createPendingEntity(orgId, clientId, orderId,
            INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_REFUND, orderData.customerName(), orderData.customerEmail(),
            buyerId, resolvedCostCenterId, advanceRefundRequestBody, lastAdvance.getFiscalbillId(),
            null); // Advance Refund never sends dateAndTimeOfIssue
        fiscalBillRepository.save(refundEntity);

        try {
            String response = taxAuthorityService.call(orgId, "CREATE_INVOICE", advanceRefundRequestBody);
            processTaxAuthorityResponse(refundEntity, response, INVOICE_TYPE_ADVANCE, TRANSACTION_TYPE_REFUND,
                    advanceLines, clientId, orgId);
            if (refundEntity.getEfiscalTotalamount() == null) {
                refundEntity.setEfiscalTotalamount(totalAdvanceAmount);
            }
            fiscalBillRepository.save(refundEntity);
            saveManualPaymentRecords(refundEntity.getFiscalbillId(), clientId, orgId, refundPayments);
            saveLineItems(refundEntity.getFiscalbillId(), clientId, orgId, advanceLines);
            log.info("Advance Refund created: {} ({} line(s), {} payment(s))",
                    refundEntity.getFiscalbillId(), advanceLines.size(), refundPayments.size());
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

    /**
     * Prefer persisted Advance Sale lines when closing the chain; fall back to the
     * incoming Normal Sale items so labels are still available.
     */
    private List<FiscalBillItemRequest> collectAdvanceSaleItems(
            List<FiscalBillEntity> advanceBills, List<FiscalBillItemRequest> fallbackItems) {
        List<FiscalBillItemRequest> fromPriorBills = new ArrayList<>();
        for (FiscalBillEntity advance : advanceBills) {
            if (advance.getFiscalbillId() == null || !STATUS_SUCCESS.equals(advance.getStatus())) {
                continue;
            }
            for (FiscalBillLineEntity line : fiscalBillLineRepository.findByFiscalbillId(advance.getFiscalbillId())) {
                fromPriorBills.add(toCopyItemRequest(line));
            }
        }
        return fromPriorBills.isEmpty() ? fallbackItems : fromPriorBills;
    }

    private List<PaymentRequest> buildAdvanceRefundPayments(
            Long clientId,
            String paymentMethodCode,
            List<PaymentRequest> sourcePayments,
            FiscalBillEntity lastAdvance,
            BigDecimal totalAdvanceAmount) {
        if (sourcePayments != null && !sourcePayments.isEmpty()) {
            return List.of(new PaymentRequest(sourcePayments.get(0).paymentType(), totalAdvanceAmount));
        }
        if (lastAdvance != null && lastAdvance.getFiscalbillId() != null) {
            List<FiscalBillPayEntity> priorPays = fiscalBillPayRepository.findByFiscalbillId(lastAdvance.getFiscalbillId());
            if (!priorPays.isEmpty() && priorPays.get(0).getPaymentType() != null) {
                return List.of(new PaymentRequest(priorPays.get(0).getPaymentType(), totalAdvanceAmount));
            }
        }
        return List.of(new PaymentRequest(resolvePaymentType(clientId, paymentMethodCode), totalAdvanceAmount));
    }

    private List<FiscalBillItemRequest> enrichItemsWithGtin(Long orgId, List<FiscalBillItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return items;
        }

        List<FiscalBillItemRequest> enriched = new ArrayList<>();
        for (FiscalBillItemRequest item : items) {
            enriched.add(new FiscalBillItemRequest(
                    item.name(),
                    item.quantity(),
                    item.unitPrice(),
                    item.totalAmount(),
                    item.taxLabel(),
                    item.taxPrefix(),
                    resolveGtinForItem(orgId, item),
                    item.productId(),
                    item.sku(),
                    item.taxValue(),
                    item.taxCategoryName(),
                    item.labels(),
                    item.totalPaid()));
        }
        return enriched;
    }

    private String resolveGtinForItem(Long orgId, FiscalBillItemRequest item) {
        String gtin = trimToNull(item.gtin());
        if (gtin != null) {
            return gtin;
        }

        Long productId = parseLongOrNull(item.productId());
        if (productId != null) {
            Optional<ProductEntity> product = productRepository.findVisibleByProductId(productId);
            String ean = product.map(ProductEntity::getEan).map(FiscalBillService::trimToNull).orElse(null);
            if (ean != null) {
                return ean;
            }
        }

        String sku = trimToNull(item.sku());
        if (sku != null) {
            Optional<ProductEntity> product = productRepository.findManualByOrgIdAndSku(orgId, sku);
            if (product.isEmpty()) {
                product = productRepository.findMerchantProByOrgIdAndSkuIncludingHidden(orgId, sku);
            }
            String ean = product.map(ProductEntity::getEan).map(FiscalBillService::trimToNull).orElse(null);
            if (ean != null) {
                return ean;
            }
        }

        return null;
    }

    private static Long parseLongOrNull(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return Long.valueOf(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
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
                labels,
                line.getTotalPaid());
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
        entity.setEfiscalLocationname(str(resp.get("locationName")));
        entity.setEfiscalDistrict(str(resp.get("district")));
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
            line.setTotalPaid(item.totalPaid());
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

    private record BuiltTaxAuthorityRequest(
            String json, Long referentFiscalbillId, String buyerId, String buyerCostCenterId,
            String dateAndTimeOfIssue) {}

    private FiscalBillEntity createPendingEntity(Long orgId, Long clientId,
            String orderId, int invoiceType, int transactionType, String customerName, String customerEmail,
            String customerId, String customerCostCenterId, String requestBody, Long referentFiscalbillId,
            String dateAndTimeOfIssue) {
        FiscalBillEntity e = new FiscalBillEntity();
        e.setOrgId(orgId);
        e.setClientId(clientId);
        e.setOrderId(orderId);
        e.setReferentFiscalbillId(referentFiscalbillId);
        e.setRequestBody(requestBody);
        e.setStatus(STATUS_PENDING);
        e.setAttemptCount(1);
        e.setEfiscalInvoicetype(invoiceType);
        e.setEfiscalTransactiontype(transactionType);
        e.setCustomerName(trimToNull(customerName));
        e.setCustomerEmail(trimToNull(customerEmail));
        e.setCustomerId(trimToNull(customerId));
        e.setCustomerCostCenterId(trimToNull(customerCostCenterId));
        e.setDateAndTimeOfIssue(trimToNull(dateAndTimeOfIssue));
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

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    static final String SHIPMENT_PRODUCT_NOT_FOUND_MESSAGE = "Nije pronadjen proizvod definisan za isporuku";

    /**
     * When the org has {@code include_shipment} enabled and the order has {@code shipping_amount > 0},
     * append a line built from the org's shipping product ({@code product.is_shipment}). The tax rate
     * comes from the order's {@code shipping_tax_percent}; the tax category name is borrowed from the
     * order's product lines so the line resolves through the same tax mapping.
     */
    private List<FiscalBillItemRequest> appendShipmentLineIfApplicable(Long orgId, OrderFiscalizeRequest orderData) {
        List<FiscalBillItemRequest> items = orderData.items() == null ? List.of() : orderData.items();
        BigDecimal shippingAmount = orderData.shippingAmount();
        if (shippingAmount == null || shippingAmount.compareTo(BigDecimal.ZERO) <= 0 || items.isEmpty()) {
            return items;
        }
        boolean includeShipment = orgRepository.findById(orgId)
                .map(org -> org.isIncludeShipment())
                .orElse(false);
        if (!includeShipment) {
            return items;
        }

        List<ProductEntity> shipmentProducts = productRepository.findShipmentProductsByOrgId(orgId);
        if (shipmentProducts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, SHIPMENT_PRODUCT_NOT_FOUND_MESSAGE);
        }
        ProductEntity product = shipmentProducts.get(0);

        String taxCategoryName = items.stream()
                .map(FiscalBillItemRequest::taxCategoryName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
        BigDecimal taxPercent = orderData.shippingTaxPercent();
        String taxPrefix = taxPercent != null ? String.format("%02d", taxPercent.intValue()) : null;

        FiscalBillItemRequest shipmentLine = new FiscalBillItemRequest(
                product.getName(),
                BigDecimal.ONE,
                shippingAmount,
                shippingAmount,
                null,
                taxPrefix,
                product.getEan(),
                product.getProductId() != null ? String.valueOf(product.getProductId()) : null,
                product.getSku(),
                taxPercent,
                taxCategoryName,
                null,
                null
        );
        List<FiscalBillItemRequest> withShipment = new ArrayList<>(items);
        withShipment.add(shipmentLine);
        return withShipment;
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
                    List.of(taxLabel),
                    item.totalPaid()
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
            request.customerEmail(),
            request.sendEmail(),
                null, // no billing type
                null, // no billing VAT
                null, // no payment method code
                request.items(),
                request.cashier(),
                request.buyerCostCenterId(),
                null, // Advance Refund never carries dateAndTimeOfIssue
                null, // manual items already include any shipping line
                null
        );
    }

    private FiscalBillView toView(FiscalBillEntity e) {
        return toView(e, null);
    }

    private FiscalBillView toView(FiscalBillEntity e, FiscalBillEmailService.EmailSendResult emailResult) {
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
                e.getUpdated() != null ? e.getUpdated().toString() : null,
                emailResult != null ? emailResult.status() : null,
                emailResult != null ? emailResult.errorMessage() : null
        );
    }

    private FiscalBillListView toListView(FiscalBillEntity e) {
        return new FiscalBillListView(
                e.getFiscalbillId(),
                e.getOrderId(),
                e.getStatus(),
                e.getCustomerName(),
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
            List<String> labels,
            BigDecimal totalPaid  // Advance Sale amount paid; optional / null for other types
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
            String customerEmail,
            boolean sendEmail,
            String billingType,          // "company" or individual
            String billingCompanyVat,
            String paymentMethodCode,    // e.g. cash_delivery, wire
            List<FiscalBillItemRequest> items,
            String cashier,              // Optional — resolved from the issuing user's cashier field
            String buyerCostCenterId,    // Optional customer field (e.g. "30:099999999"); applied only with buyerId
            String dateAndTimeOfIssue,   // Optional advance payment moment; Advance Sale only
            BigDecimal shippingAmount,   // Optional order shipping_amount (gross)
            BigDecimal shippingTaxPercent // Optional order shipping_tax_percent
    ) {}

    /** Request object for manual fiscal bill creation. */
    public record ManualFiscalBillRequest(
            String orderId,             // Optional — if set, applies order-based checks
            String customerName,
            String customerEmail,
            boolean sendEmail,
            int invoiceType,
            int transactionType,
            String buyerId,             // Optional full buyer identifier (e.g. "10:123456789")
            String buyerType,           // Optional buyer type prefix (e.g. "10")
            String buyerVat,            // Optional company VAT
            String buyerCostCenterId,   // Optional customer field (e.g. "30:099999999")
            List<FiscalBillItemRequest> items,
            List<PaymentRequest> payments,
            String referentDocumentNumber, // Optional — user-supplied reference for Copy/Refund/Advance chain
            String cashier,             // Optional — resolved from the issuing user's cashier field
            String dateAndTimeOfIssue   // Optional advance payment moment; Advance Sale only
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
            String updatedAt,
            String emailStatus,
            String emailError
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

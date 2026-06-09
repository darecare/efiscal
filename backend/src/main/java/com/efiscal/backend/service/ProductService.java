package com.efiscal.backend.service;

import com.efiscal.backend.model.OrgEntity;
import com.efiscal.backend.model.ProductEntity;
import static com.efiscal.backend.model.ProductEntity.SOURCE_TYPE_MANUAL;
import static com.efiscal.backend.model.ProductEntity.SOURCE_TYPE_MERCHANTPRO;
import static com.efiscal.backend.model.ProductEntity.SYNC_STATUS_ACTIVE;
import com.efiscal.backend.repository.OrgRepository;
import com.efiscal.backend.repository.ProductRepository;
import com.efiscal.backend.service.MerchantProProductService.MerchantProProductRow;
import com.efiscal.backend.service.MerchantProProductService.ProductFetchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.efiscal.backend.service.ProductSyncJobService.SyncStartDecision;
import com.efiscal.backend.service.ProductSyncJobService.SyncStatusDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ProductService {

    private static final int SYNC_PAGE_SIZE = 100;
    private static final long SSE_TIMEOUT_MS = 0L;
    private static final long SYNC_PAGE_THROTTLE_MS = 750L;
    private static final int SEARCH_MAX_RESULTS = 50;
    private static final int LIST_IDS_MAX = 5000;
    private static final int BULK_MAX_IDS = 500;
    static final String INCREMENTAL_FILTER_UNSUPPORTED = "INCREMENTAL_FILTER_UNSUPPORTED";

    private final ProductRepository productRepository;
    private final OrgRepository orgRepository;
    private final MerchantProProductService merchantProProductService;
    private final ProductSyncJobService productSyncJobService;
    private final ObjectMapper objectMapper;
    private final ProductService self;

    public ProductService(
        ProductRepository productRepository,
        OrgRepository orgRepository,
        MerchantProProductService merchantProProductService,
        ProductSyncJobService productSyncJobService,
        ObjectMapper objectMapper,
        @Lazy ProductService self
    ) {
        this.productRepository = productRepository;
        this.orgRepository = orgRepository;
        this.merchantProProductService = merchantProProductService;
        this.productSyncJobService = productSyncJobService;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @Transactional(readOnly = true)
    public SyncStatusDto getSyncStatus(Long orgId) {
        requireOrg(orgId);
        return productSyncJobService.getStatus(orgId);
    }

    @Transactional
    public void cancelSync(Long orgId) {
        requireOrg(orgId);
        productSyncJobService.findRunningJob(orgId).ifPresent(job ->
            productSyncJobService.completeJob(
                job.getSyncJobId(),
                ProductSyncJobService.STATUS_FAILED,
                "Cancelled by user"
            )
        );
    }

    @Transactional(readOnly = true)
    public ProductPage listByOrg(Long orgId, int page, int size, String q) {
        requireOrg(orgId);
        int safeSize = Math.min(Math.max(size, 1), 500);
        int safePage = Math.max(page, 0);
        var pageable = PageRequest.of(safePage, safeSize);
        String term = normalizeSearchTerm(q);
        var result = term == null
            ? productRepository.findAllVisibleByOrgIdOrderByNameAsc(orgId, pageable)
            : productRepository.findAllVisibleByOrgIdAndSearchTerm(orgId, term, pageable);
        List<ProductDto> items = result.getContent().stream().map(this::toDto).toList();
        return new ProductPage(items, result.getTotalElements(), safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public ProductIdsResponse listIdsByOrg(Long orgId, String q) {
        requireOrg(orgId);
        String term = normalizeSearchTerm(q);
        var pageable = PageRequest.of(0, LIST_IDS_MAX);
        List<Long> ids = term == null
            ? productRepository.findVisibleIdsByOrgId(orgId, pageable)
            : productRepository.findVisibleIdsByOrgIdAndSearchTerm(orgId, term, pageable);
        return new ProductIdsResponse(ids);
    }

    @Transactional(readOnly = true)
    public ProductDto get(Long productId) {
        return toDto(requireProduct(productId));
    }

    @Transactional(readOnly = true)
    public List<ProductDto> search(Long orgId, String q, String name, String sku, String ean) {
        requireOrg(orgId);
        var pageable = PageRequest.of(0, SEARCH_MAX_RESULTS);
        String term = blankToNull(q);
        if (term != null) {
            return productRepository.searchByTerm(orgId, term, pageable)
                .stream()
                .map(this::toDto)
                .toList();
        }
        return productRepository.search(orgId, blankToNull(name), blankToNull(sku), blankToNull(ean), pageable)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public ProductDto create(Long orgId, ProductRequest req) {
        OrgEntity org = requireOrg(orgId);
        validateRequest(req);

        ProductEntity entity = new ProductEntity();
        entity.setClientId(org.getClient().getClientId());
        entity.setOrgId(orgId);
        entity.setName(req.name().trim());
        entity.setSku(trimOrNull(req.sku()));
        entity.setEan(trimOrNull(req.ean()));
        entity.setLastKnownPrice(req.lastKnownPrice());
        entity.setActive(req.isActive() == null || req.isActive());
        entity.setSourceType(SOURCE_TYPE_MANUAL);
        entity.setSyncStatus(SYNC_STATUS_ACTIVE);
        return toDto(productRepository.save(entity));
    }

    @Transactional
    public ProductDto update(Long productId, ProductRequest req) {
        ProductEntity entity = requireProduct(productId);
        validateRequest(req);

        entity.setName(req.name().trim());
        entity.setSku(trimOrNull(req.sku()));
        entity.setEan(trimOrNull(req.ean()));
        entity.setLastKnownPrice(req.lastKnownPrice());
        if (req.isActive() != null) {
            entity.setActive(req.isActive());
        }
        return toDto(productRepository.save(entity));
    }

    @Transactional
    public void softDelete(Long productId) {
        ProductEntity entity = requireProduct(productId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (SOURCE_TYPE_MERCHANTPRO.equals(entity.getSourceType())) {
            entity.setHiddenAt(now);
        } else {
            entity.setDeletedAt(now);
        }
        productRepository.save(entity);
    }

    @Transactional
    public BulkDeleteResult softDeleteMany(Long orgId, BulkProductIdsRequest req) {
        requireOrg(orgId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (Boolean.TRUE.equals(req.selectAll())) {
            String term = normalizeSearchTerm(req.q());
            int hidden = productRepository.hideAllSyncedByOrgIdAndOptionalSearchTerm(orgId, term, now);
            int deleted = productRepository.softDeleteAllManualByOrgIdAndOptionalSearchTerm(orgId, term, now);
            return new BulkDeleteResult(hidden + deleted);
        }
        List<Long> safeIds = validateBulkIds(req.productIds());
        int hidden = productRepository.hideSyncedByIdsAndOrgId(safeIds, orgId, now);
        int deleted = productRepository.softDeleteManualByIdsAndOrgId(safeIds, orgId, now);
        return new BulkDeleteResult(hidden + deleted);
    }

    @Transactional
    public BulkStatusResult updateStatusMany(Long orgId, BulkStatusRequest req) {
        requireOrg(orgId);
        if (Boolean.TRUE.equals(req.selectAll())) {
            String term = normalizeSearchTerm(req.q());
            int updated = productRepository.updateStatusAllVisibleByOrgIdAndOptionalSearchTerm(
                orgId,
                term,
                req.isActive()
            );
            return new BulkStatusResult(updated);
        }
        List<Long> safeIds = validateBulkIds(req.productIds());
        int updated = productRepository.updateStatusVisibleByIdsAndOrgId(safeIds, orgId, req.isActive());
        return new BulkStatusResult(updated);
    }

    public SseEmitter syncFromShopStream(Long orgId, String mode) {
        OrgEntity org = requireOrg(orgId);
        SyncStartDecision decision = productSyncJobService.resolveSyncStart(orgId, mode);
        long jobId = productSyncJobService.startJob(orgId, decision.syncType(), decision.filterFrom());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(() -> {});
        emitter.onError(ex -> {});

        long clientId = org.getClient().getClientId();
        LocalDate modifiedSince = decision.modifiedSince();
        String syncType = decision.syncType();
        java.util.concurrent.CompletableFuture.runAsync(
            () -> runSyncStream(orgId, clientId, jobId, modifiedSince, syncType, emitter));
        return emitter;
    }

    private void runSyncStream(
        Long orgId,
        long clientId,
        long jobId,
        LocalDate modifiedSince,
        String syncType,
        SseEmitter emitter
    ) {
        int synced = 0;
        Integer total = null;
        Set<Long> seenMpProductIds = new HashSet<>();
        boolean fullCatalogSync = ProductSyncJobService.isFullCatalogSync(syncType);
        try {
            int start = 0;

            while (true) {
                ProductFetchResult page = merchantProProductService.fetchProducts(
                    orgId, start, SYNC_PAGE_SIZE, modifiedSince);

                if (total == null) {
                    total = page.total() > 0 ? page.total() : null;
                }

                List<MerchantProProductRow> rows = page.data().stream()
                    .filter(row -> row.name() != null && !row.name().isBlank())
                    .toList();

                if (!rows.isEmpty()) {
                    UpsertPageResult pageResult = self.upsertPage(orgId, clientId, rows, syncType);
                    synced += pageResult.count();
                    seenMpProductIds.addAll(pageResult.seenMpProductIds());
                }

                int reportedTotal = total != null ? total : synced;
                productSyncJobService.updateProgress(jobId, synced, reportedTotal);
                sendProgress(emitter, new SyncProgress(synced, reportedTotal, false, syncType));

                if (page.nextLink() == null || page.data().isEmpty()) {
                    break;
                }
                start += SYNC_PAGE_SIZE;
                Thread.sleep(SYNC_PAGE_THROTTLE_MS);
            }

            if (fullCatalogSync) {
                self.markMissingAfterFullSync(orgId, seenMpProductIds);
            }

            int finalTotal = total != null ? total : synced;
            productSyncJobService.updateProgress(jobId, synced, finalTotal);
            productSyncJobService.completeJob(jobId, ProductSyncJobService.STATUS_DONE, null);
            sendProgress(emitter, new SyncProgress(synced, finalTotal, true, syncType));
            try { emitter.complete(); } catch (Exception ignored) {}
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            failSyncJob(emitter, jobId, synced, total, syncType, "Product sync interrupted");
        } catch (ResponseStatusException ex) {
            failSyncJob(emitter, jobId, synced, total, syncType, mapSyncFailureMessage(ex));
        } catch (Exception ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            failSyncJob(emitter, jobId, synced, total, syncType, message);
        }
    }

    private void failSyncJob(
        SseEmitter emitter,
        long jobId,
        int synced,
        Integer total,
        String syncType,
        String message
    ) {
        productSyncJobService.completeJob(jobId, ProductSyncJobService.STATUS_FAILED, message);
        int reportedTotal = total != null ? total : synced;
        sendProgress(emitter, new SyncProgress(synced, reportedTotal, true, syncType, message));
        try { emitter.complete(); } catch (Exception ignored) {}
    }

    private static String mapSyncFailureMessage(ResponseStatusException ex) {
        String reason = ex.getReason();
        if (reason == null || reason.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        if (isUndefinedFilterError(reason)) {
            return INCREMENTAL_FILTER_UNSUPPORTED + ": " + reason;
        }
        return reason;
    }

    private static boolean isUndefinedFilterError(String message) {
        return message.contains("No such filter") || message.contains("undefined_filter");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UpsertPageResult upsertPage(
        Long orgId,
        long clientId,
        List<MerchantProProductRow> rows,
        String syncType
    ) {
        boolean resetFull = ProductSyncJobService.isResetFullSync(syncType);
        List<Long> seenMpProductIds = new ArrayList<>();
        int count = 0;
        for (MerchantProProductRow row : rows) {
            Long mpProductId = upsertFromMerchantPro(orgId, clientId, row, resetFull);
            if (mpProductId != null) {
                seenMpProductIds.add(mpProductId);
            }
            count++;
        }
        return new UpsertPageResult(count, seenMpProductIds);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markMissingAfterFullSync(Long orgId, Set<Long> seenMpProductIds) {
        if (seenMpProductIds.isEmpty()) {
            productRepository.markAllMerchantProMissingInSource(orgId);
        } else {
            productRepository.markMissingInSourceExcept(orgId, seenMpProductIds);
        }
    }

    @Transactional(readOnly = true)
    public LivePriceLookupResult lookupLivePrice(Long orgId, String sku, String ean) {
        requireOrg(orgId);
        String skuVal = trimOrNull(sku);
        String eanVal = trimOrNull(ean);
        if ((skuVal == null || skuVal.isBlank()) && (eanVal == null || eanVal.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "SKU or EAN is required for live price lookup");
        }

        Optional<MerchantProProductRow> row = Optional.empty();
        if (skuVal != null) {
            row = merchantProProductService.fetchInventoryByIdentifier(orgId, "sku", skuVal);
        }
        if (row.isEmpty() && eanVal != null) {
            row = merchantProProductService.fetchInventoryByIdentifier(orgId, "ean", eanVal);
        }
        if (row.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Product not found in shop for the given SKU or EAN");
        }

        MerchantProProductRow found = row.get();
        return new LivePriceLookupResult(
            found.name(),
            found.sku(),
            found.ean(),
            found.priceGross(),
            found.mpProductId()
        );
    }

    private void sendProgress(SseEmitter emitter, SyncProgress progress) {
        try {
            String json = objectMapper.writeValueAsString(progress);
            emitter.send(SseEmitter.event().data(json, MediaType.APPLICATION_JSON));
        } catch (Exception ignored) {
            // Client disconnected; worker continues and job stays RUNNING in DB
        }
    }

    private Long upsertFromMerchantPro(
        Long orgId,
        long clientId,
        MerchantProProductRow row,
        boolean resetFull
    ) {
        ProductEntity entity = null;
        if (row.mpProductId() != null) {
            entity = productRepository
                .findByOrgIdAndMpProductIdIncludingHidden(orgId, row.mpProductId())
                .orElse(null);
        }

        String skuVal = trimOrNull(row.sku());
        if (entity == null && skuVal != null) {
            entity = productRepository
                .findMerchantProByOrgIdAndSkuIncludingHidden(orgId, skuVal)
                .orElse(null);
        }

        String eanVal = trimOrNull(row.ean());
        if (entity == null && eanVal != null) {
            entity = productRepository
                .findMerchantProByOrgIdAndEanIncludingHidden(orgId, eanVal)
                .orElse(null);
        }

        if (entity == null) {
            entity = new ProductEntity();
            entity.setClientId(clientId);
            entity.setOrgId(orgId);
            entity.setSourceType(SOURCE_TYPE_MERCHANTPRO);
        }

        entity.setMpProductId(row.mpProductId());
        entity.setSourceType(SOURCE_TYPE_MERCHANTPRO);
        entity.setName(row.name());
        entity.setSku(skuVal);
        entity.setEan(eanVal);
        entity.setLastKnownPrice(row.priceGross());
        entity.setActive(true);
        entity.setSyncStatus(SYNC_STATUS_ACTIVE);
        entity.setDeletedAt(null);
        if (resetFull) {
            entity.setHiddenAt(null);
        }
        productRepository.save(entity);
        return row.mpProductId();
    }

    private OrgEntity requireOrg(Long orgId) {
        return orgRepository.findById(orgId)
            .filter(o -> o.getDeletedAt() == null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    private ProductEntity requireProduct(Long productId) {
        return productRepository.findVisibleByProductId(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void validateRequest(ProductRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is required");
        }
        validateIdentifiers(req.sku(), req.ean());
    }

    private void validateIdentifiers(String sku, String ean) {
        String skuVal = trimOrNull(sku);
        String eanVal = trimOrNull(ean);
        if ((skuVal == null || skuVal.isBlank()) && (eanVal == null || eanVal.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "At least one of SKU or EAN is required");
        }
    }

    private ProductDto toDto(ProductEntity entity) {
        return new ProductDto(
            entity.getProductId(),
            entity.getClientId(),
            entity.getOrgId(),
            entity.getMpProductId(),
            entity.getName(),
            entity.getSku(),
            entity.getEan(),
            entity.getLastKnownPrice(),
            entity.isActive()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeSearchTerm(String q) {
        String trimmed = blankToNull(q);
        if (trimmed == null) {
            return null;
        }
        return "%" + trimmed.toLowerCase() + "%";
    }

    private static List<Long> validateBulkIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one product ID is required");
        }
        List<Long> distinct = ids.stream().distinct().toList();
        if (distinct.size() > BULK_MAX_IDS) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Too many product IDs (max " + BULK_MAX_IDS + ")"
            );
        }
        return distinct;
    }

    private static String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record ProductDto(
        Long productId,
        Long clientId,
        Long orgId,
        Long mpProductId,
        String name,
        String sku,
        String ean,
        BigDecimal lastKnownPrice,
        boolean isActive
    ) {}

    public record ProductPage(
        List<ProductDto> items,
        long totalCount,
        int page,
        int size
    ) {}

    public record ProductRequest(
        String name,
        String sku,
        String ean,
        BigDecimal lastKnownPrice,
        Boolean isActive
    ) {}

    public record SyncProgress(int synced, int total, boolean done, String syncType, String error) {
        public SyncProgress(int synced, int total, boolean done, String syncType) {
            this(synced, total, done, syncType, null);
        }
    }

    public record LivePriceLookupResult(
        String name,
        String sku,
        String ean,
        BigDecimal priceGross,
        Long mpProductId
    ) {}

    public record ProductIdsResponse(List<Long> productIds) {}

    public record BulkProductIdsRequest(List<Long> productIds, Boolean selectAll, String q) {}

    public record BulkStatusRequest(List<Long> productIds, boolean isActive, Boolean selectAll, String q) {}

    public record BulkDeleteResult(int deleted) {}

    public record BulkStatusResult(int updated) {}

    public record UpsertPageResult(int count, List<Long> seenMpProductIds) {}
}

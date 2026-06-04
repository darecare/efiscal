package com.efiscal.backend.service;

import com.efiscal.backend.model.OrgEntity;
import com.efiscal.backend.model.ProductEntity;
import com.efiscal.backend.repository.OrgRepository;
import com.efiscal.backend.repository.ProductRepository;
import com.efiscal.backend.service.MerchantProProductService.MerchantProProductRow;
import com.efiscal.backend.service.MerchantProProductService.ProductFetchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.context.annotation.Lazy;
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
    private static final long SSE_TIMEOUT_MS = 300_000L;

    private final ProductRepository productRepository;
    private final OrgRepository orgRepository;
    private final MerchantProProductService merchantProProductService;
    private final ObjectMapper objectMapper;
    private final ProductService self;

    public ProductService(
        ProductRepository productRepository,
        OrgRepository orgRepository,
        MerchantProProductService merchantProProductService,
        ObjectMapper objectMapper,
        @Lazy ProductService self
    ) {
        this.productRepository = productRepository;
        this.orgRepository = orgRepository;
        this.merchantProProductService = merchantProProductService;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> listByOrg(Long orgId) {
        return productRepository.findAllByOrgIdAndDeletedAtIsNullOrderByNameAsc(orgId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProductDto get(Long productId) {
        return toDto(requireProduct(productId));
    }

    @Transactional(readOnly = true)
    public List<ProductDto> search(Long orgId, String q, String name, String sku, String ean) {
        requireOrg(orgId);
        String term = blankToNull(q);
        if (term != null) {
            return productRepository.searchByTerm(orgId, term)
                .stream()
                .map(this::toDto)
                .toList();
        }
        return productRepository.search(orgId, blankToNull(name), blankToNull(sku), blankToNull(ean))
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
        entity.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        productRepository.save(entity);
    }

    public SseEmitter syncFromShopStream(Long orgId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        emitter.onError(ex -> emitter.complete());

        OrgEntity org = requireOrg(orgId);
        java.util.concurrent.CompletableFuture.runAsync(() -> runSyncStream(org, emitter));
        return emitter;
    }

    private void runSyncStream(OrgEntity org, SseEmitter emitter) {
        try {
            int start = 0;
            int synced = 0;
            Integer total = null;

            while (true) {
                ProductFetchResult page = merchantProProductService.fetchProducts(
                    org.getOrgId(), null, null, start, SYNC_PAGE_SIZE);

                if (total == null) {
                    total = page.total() > 0 ? page.total() : null;
                }

                List<MerchantProProductRow> rows = page.data().stream()
                    .filter(row -> row.name() != null && !row.name().isBlank())
                    .toList();

                if (!rows.isEmpty()) {
                    synced += self.upsertPage(org, rows);
                }

                int reportedTotal = total != null ? total : synced;
                sendProgress(emitter, new SyncProgress(synced, reportedTotal, false));

                if (page.data().isEmpty() || page.data().size() < SYNC_PAGE_SIZE) {
                    break;
                }
                start += SYNC_PAGE_SIZE;
            }

            int finalTotal = total != null ? total : synced;
            sendProgress(emitter, new SyncProgress(synced, finalTotal, true));
            emitter.complete();
        } catch (Exception ex) {
            try {
                emitter.completeWithError(ex);
            } catch (Exception ignored) {
                emitter.complete();
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int upsertPage(OrgEntity org, List<MerchantProProductRow> rows) {
        int count = 0;
        for (MerchantProProductRow row : rows) {
            upsertFromMerchantPro(org, row);
            count++;
        }
        return count;
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

        ProductFetchResult result = merchantProProductService.fetchProducts(orgId, skuVal, null, 0, 1);
        if (result.data().isEmpty() && eanVal != null) {
            result = merchantProProductService.fetchProducts(orgId, null, eanVal, 0, 1);
        }
        if (result.data().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Product not found in shop for the given SKU or EAN");
        }

        MerchantProProductRow row = result.data().get(0);
        return new LivePriceLookupResult(
            row.name(),
            row.sku(),
            row.ean(),
            row.priceGross(),
            row.mpProductId()
        );
    }

    private void sendProgress(SseEmitter emitter, SyncProgress progress) throws IOException {
        String json = objectMapper.writeValueAsString(progress);
        emitter.send(SseEmitter.event().data(json, MediaType.APPLICATION_JSON));
    }

    private void upsertFromMerchantPro(OrgEntity org, MerchantProProductRow row) {
        ProductEntity entity = null;
        if (row.mpProductId() != null) {
            entity = productRepository
                .findByOrgIdAndMpProductIdAndDeletedAtIsNull(org.getOrgId(), row.mpProductId())
                .orElse(null);
        }

        String skuVal = trimOrNull(row.sku());
        if (entity == null && skuVal != null) {
            entity = productRepository
                .findByOrgIdAndSkuIgnoreCaseAndDeletedAtIsNull(org.getOrgId(), skuVal)
                .orElse(null);
        }

        String eanVal = trimOrNull(row.ean());
        if (entity == null && eanVal != null) {
            entity = productRepository
                .findByOrgIdAndEanAndDeletedAtIsNull(org.getOrgId(), eanVal)
                .orElse(null);
        }

        if (entity == null) {
            entity = new ProductEntity();
            entity.setClientId(org.getClient().getClientId());
            entity.setOrgId(org.getOrgId());
            entity.setMpProductId(row.mpProductId());
        }

        entity.setName(row.name());
        entity.setSku(skuVal);
        entity.setEan(eanVal);
        entity.setLastKnownPrice(row.priceGross());
        entity.setActive(true);
        entity.setDeletedAt(null);
        productRepository.save(entity);
    }

    private OrgEntity requireOrg(Long orgId) {
        return orgRepository.findById(orgId)
            .filter(o -> o.getDeletedAt() == null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    private ProductEntity requireProduct(Long productId) {
        return productRepository.findByProductIdAndDeletedAtIsNull(productId)
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
        Integer mpProductId,
        String name,
        String sku,
        String ean,
        BigDecimal lastKnownPrice,
        boolean isActive
    ) {}

    public record ProductRequest(
        String name,
        String sku,
        String ean,
        BigDecimal lastKnownPrice,
        Boolean isActive
    ) {}

    public record SyncProgress(int synced, int total, boolean done) {}

    public record LivePriceLookupResult(
        String name,
        String sku,
        String ean,
        BigDecimal priceGross,
        Integer mpProductId
    ) {}
}

package com.efiscal.backend.controller;

import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.ProductService;
import com.efiscal.backend.service.ProductService.BulkDeleteResult;
import com.efiscal.backend.service.ProductService.BulkProductIdsRequest;
import com.efiscal.backend.service.ProductService.BulkStatusRequest;
import com.efiscal.backend.service.ProductService.BulkStatusResult;
import com.efiscal.backend.service.ProductService.LivePriceLookupResult;
import com.efiscal.backend.service.ProductService.ProductDto;
import com.efiscal.backend.service.ProductService.ProductIdsResponse;
import com.efiscal.backend.service.ProductService.ProductPage;
import com.efiscal.backend.service.ProductService.ProductRequest;
import com.efiscal.backend.service.ProductSyncJobService.SyncStatusDto;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    private final AuthorizationService authorizationService;

    public ProductController(ProductService productService, AuthorizationService authorizationService) {
        this.productService = productService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ProductPage list(
        @RequestParam Long orgId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size,
        @RequestParam(required = false) String q
    ) {
        authorizationService.requireAction("FISCAL_MANAGE_PRODUCTS");
        authorizationService.requireOrgAccess(orgId);
        return productService.listByOrg(orgId, page, size, q);
    }

    @GetMapping("/ids")
    public ProductIdsResponse listIds(
        @RequestParam Long orgId,
        @RequestParam(required = false) String q
    ) {
        authorizationService.requireAction("FISCAL_MANAGE_PRODUCTS");
        authorizationService.requireOrgAccess(orgId);
        return productService.listIdsByOrg(orgId, q);
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(
        @RequestParam Long orgId,
        @RequestBody ProductRequest req
    ) {
        authorizationService.requireAction("FISCAL_MANAGE_PRODUCTS");
        authorizationService.requireOrgAccess(orgId);
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(orgId, req));
    }

    @PutMapping("/{id}")
    public ProductDto update(@PathVariable Long id, @RequestBody ProductRequest req) {
        authorizationService.requireAction("FISCAL_MANAGE_PRODUCTS");
        ProductDto existing = productService.get(id);
        authorizationService.requireOrgAccess(existing.orgId());
        return productService.update(id, req);
    }

    @DeleteMapping("/bulk")
    public BulkDeleteResult bulkDelete(
        @RequestParam Long orgId,
        @RequestBody BulkProductIdsRequest req
    ) {
        authorizationService.requireAction("FISCAL_MANAGE_PRODUCTS");
        authorizationService.requireOrgAccess(orgId);
        return productService.softDeleteMany(orgId, req);
    }

    @PatchMapping("/bulk/status")
    public BulkStatusResult bulkUpdateStatus(
        @RequestParam Long orgId,
        @RequestBody BulkStatusRequest req
    ) {
        authorizationService.requireAction("FISCAL_MANAGE_PRODUCTS");
        authorizationService.requireOrgAccess(orgId);
        return productService.updateStatusMany(orgId, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorizationService.requireAction("FISCAL_MANAGE_PRODUCTS");
        ProductDto existing = productService.get(id);
        authorizationService.requireOrgAccess(existing.orgId());
        productService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<ProductDto> search(
        @RequestParam Long orgId,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String sku,
        @RequestParam(required = false) String ean
    ) {
        authorizationService.requireAction("FISCAL_CREATE_BILL");
        authorizationService.requireOrgAccess(orgId);
        return productService.search(orgId, q, name, sku, ean);
    }

    @GetMapping("/sync/status")
    public SyncStatusDto syncStatus(@RequestParam Long orgId) {
        authorizationService.requireAction("FISCAL_MANAGE_PRODUCTS");
        authorizationService.requireOrgAccess(orgId);
        return productService.getSyncStatus(orgId);
    }

    @GetMapping(
        value = "/sync",
        produces = { MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    public Object sync(
        @RequestParam Long orgId,
        @RequestParam(defaultValue = "AUTO") String mode
    ) {
        authorizationService.requireAction("FISCAL_MANAGE_PRODUCTS");
        authorizationService.requireOrgAccess(orgId);
        try {
            return productService.syncFromShopStream(orgId, mode);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(productService.getSyncStatus(orgId));
            }
            throw ex;
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(productService.getSyncStatus(orgId));
        }
    }

    @PostMapping("/sync/cancel")
    public ResponseEntity<Void> cancelSync(@RequestParam Long orgId) {
        authorizationService.requireAction("FISCAL_MANAGE_PRODUCTS");
        authorizationService.requireOrgAccess(orgId);
        productService.cancelSync(orgId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lookup")
    public LivePriceLookupResult lookup(
        @RequestParam Long orgId,
        @RequestParam(required = false) String sku,
        @RequestParam(required = false) String ean
    ) {
        authorizationService.requireAction("FISCAL_CREATE_BILL");
        authorizationService.requireOrgAccess(orgId);
        return productService.lookupLivePrice(orgId, sku, ean);
    }
}

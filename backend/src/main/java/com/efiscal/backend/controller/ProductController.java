package com.efiscal.backend.controller;

import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.ProductService;
import com.efiscal.backend.service.ProductService.LivePriceLookupResult;
import com.efiscal.backend.service.ProductService.ProductDto;
import com.efiscal.backend.service.ProductService.ProductPage;
import com.efiscal.backend.service.ProductService.ProductRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
        @RequestParam(defaultValue = "100") int size
    ) {
        authorizationService.requireAction("FISCAL_MANAGE_PRODUCTS");
        authorizationService.requireOrgAccess(orgId);
        return productService.listByOrg(orgId, page, size);
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

    @GetMapping("/sync")
    public SseEmitter sync(@RequestParam Long orgId) {
        authorizationService.requireAction("FISCAL_MANAGE_PRODUCTS");
        authorizationService.requireOrgAccess(orgId);
        return productService.syncFromShopStream(orgId);
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

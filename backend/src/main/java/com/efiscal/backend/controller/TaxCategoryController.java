package com.efiscal.backend.controller;

import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.TaxCategoryService;
import com.efiscal.backend.service.TaxCategoryService.TaxCategoryDto;
import com.efiscal.backend.service.TaxCategoryService.TaxCategoryRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tax-categories")
public class TaxCategoryController {

    private final TaxCategoryService taxCategoryService;
    private final AuthorizationService authorizationService;

    public TaxCategoryController(TaxCategoryService taxCategoryService, AuthorizationService authorizationService) {
        this.taxCategoryService = taxCategoryService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<TaxCategoryDto> listTaxCategories() {
        authorizationService.requireAction("ORGS_MANAGE");
        return taxCategoryService.listTaxCategories();
    }

    @PostMapping
    public ResponseEntity<?> createTaxCategory(@RequestBody TaxCategoryRequest req) {
        authorizationService.requireAction("ORGS_MANAGE");
        return ResponseEntity.status(HttpStatus.CREATED).body(taxCategoryService.createTaxCategory(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTaxCategory(@PathVariable Long id, @RequestBody TaxCategoryRequest req) {
        authorizationService.requireAction("ORGS_MANAGE");
        return ResponseEntity.ok(taxCategoryService.updateTaxCategory(id, req));
    }
}

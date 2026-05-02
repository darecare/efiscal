package com.efiscal.backend.controller;

import com.efiscal.backend.service.DemoDataService;
import com.efiscal.backend.service.TaxCategoryService;
import com.efiscal.backend.service.TaxCategoryService.TaxCategoryDto;
import com.efiscal.backend.service.TaxCategoryService.TaxCategoryRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tax-categories")
public class TaxCategoryController {

    private final TaxCategoryService taxCategoryService;

    public TaxCategoryController(TaxCategoryService taxCategoryService) {
        this.taxCategoryService = taxCategoryService;
    }

    @GetMapping
    public List<TaxCategoryDto> listTaxCategories() {
        return taxCategoryService.listTaxCategories();
    }

    @PostMapping
    public ResponseEntity<?> createTaxCategory(@RequestBody TaxCategoryRequest req) {
        if (!isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(taxCategoryService.createTaxCategory(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTaxCategory(@PathVariable Long id, @RequestBody TaxCategoryRequest req) {
        if (!isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        }
        return ResponseEntity.ok(taxCategoryService.updateTaxCategory(id, req));
    }

    private boolean isSuperAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        Object principal = auth.getPrincipal();
        if (principal instanceof DemoDataService.AuthenticatedUser u) {
            return "SUPERADMIN".equals(u.roleName());
        }
        return false;
    }
}

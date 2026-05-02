package com.efiscal.backend.controller;

import com.efiscal.backend.service.DemoDataService;
import com.efiscal.backend.service.TaxService;
import com.efiscal.backend.service.TaxService.TaxDto;
import com.efiscal.backend.service.TaxService.TaxRequest;
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
@RequestMapping("/api/v1/taxes")
public class TaxController {

    private final TaxService taxService;

    public TaxController(TaxService taxService) {
        this.taxService = taxService;
    }

    @GetMapping
    public List<TaxDto> listTaxes() {
        return taxService.listTaxes();
    }

    @PostMapping
    public ResponseEntity<?> createTax(@RequestBody TaxRequest req) {
        if (!isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(taxService.createTax(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTax(@PathVariable Long id, @RequestBody TaxRequest req) {
        if (!isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        }
        return ResponseEntity.ok(taxService.updateTax(id, req));
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

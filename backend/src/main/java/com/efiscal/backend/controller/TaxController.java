package com.efiscal.backend.controller;

import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.TaxService;
import com.efiscal.backend.service.TaxService.TaxDto;
import com.efiscal.backend.service.TaxService.TaxRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/taxes")
public class TaxController {

    private final TaxService taxService;
    private final AuthorizationService authorizationService;

    public TaxController(TaxService taxService, AuthorizationService authorizationService) {
        this.taxService = taxService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<TaxDto> listTaxes() {
        authorizationService.requireAction("ORGS_MANAGE");
        return taxService.listTaxes();
    }

    @PostMapping
    public ResponseEntity<?> createTax(@RequestBody TaxRequest req) {
        authorizationService.requireAction("ORGS_MANAGE");
        return ResponseEntity.status(HttpStatus.CREATED).body(taxService.createTax(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTax(@PathVariable Long id, @RequestBody TaxRequest req) {
        authorizationService.requireAction("ORGS_MANAGE");
        return ResponseEntity.ok(taxService.updateTax(id, req));
    }
}

package com.efiscal.backend.controller;

import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.DemoDataService;
import com.efiscal.backend.service.OrgService;
import com.efiscal.backend.service.OrgService.OrgDto;
import com.efiscal.backend.service.OrgService.OrgRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orgs")
public class OrgController {

    private final OrgService orgService;
    private final AuthorizationService authorizationService;

    public OrgController(OrgService orgService, AuthorizationService authorizationService) {
        this.orgService = orgService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<OrgDto> listOrgs(@RequestParam(required = false) Long clientId) {
        authorizationService.requireAction("ORGS_MANAGE");
        return orgService.listOrgs(clientId);
    }

    @GetMapping("/my-access")
    public List<OrgDto> myAccessOrgs() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof DemoDataService.AuthenticatedUser u)) {
            return List.of();
        }
        boolean isSuperAdmin = authorizationService.isSuperAdmin();
        return orgService.listMyOrgs(u.email(), isSuperAdmin);
    }

    @GetMapping("/{orgId}")
    public OrgDto getOrg(@PathVariable Long orgId) {
        authorizationService.requireAction("ORGS_MANAGE");
        return orgService.getOrg(orgId);
    }

    @PostMapping
    public ResponseEntity<?> createOrg(@RequestBody OrgRequest req) {
        authorizationService.requireAction("ORGS_MANAGE");
        return ResponseEntity.status(HttpStatus.CREATED).body(orgService.createOrg(req));
    }

    @PutMapping("/{orgId}")
    public ResponseEntity<?> updateOrg(@PathVariable Long orgId, @RequestBody OrgRequest req) {
        authorizationService.requireAction("ORGS_MANAGE");
        return ResponseEntity.ok(orgService.updateOrg(orgId, req));
    }

    @GetMapping("/{orgId}/payment-types")
    public List<Integer> getOrgPaymentTypes(@PathVariable Long orgId) {
        return orgService.getOrgPaymentTypes(orgId);
    }

    @PostMapping("/{orgId}/payment-types")
    public ResponseEntity<?> setOrgPaymentTypes(@PathVariable Long orgId, @RequestBody List<Integer> paymentTypes) {
        authorizationService.requireSuperAdmin();
        orgService.setOrgPaymentTypes(orgId, paymentTypes);
        return ResponseEntity.ok().build();
    }
}

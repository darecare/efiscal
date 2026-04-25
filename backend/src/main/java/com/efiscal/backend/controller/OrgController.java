package com.efiscal.backend.controller;

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

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    @GetMapping
    public List<OrgDto> listOrgs(@RequestParam(required = false) Long clientId) {
        return orgService.listOrgs(clientId);
    }

    @GetMapping("/{orgId}")
    public OrgDto getOrg(@PathVariable Long orgId) {
        return orgService.getOrg(orgId);
    }

    @PostMapping
    public ResponseEntity<?> createOrg(@RequestBody OrgRequest req) {
        if (!isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(orgService.createOrg(req));
    }

    @PutMapping("/{orgId}")
    public ResponseEntity<?> updateOrg(@PathVariable Long orgId, @RequestBody OrgRequest req) {
        if (!isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        }
        return ResponseEntity.ok(orgService.updateOrg(orgId, req));
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

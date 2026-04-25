package com.efiscal.backend.controller;

import com.efiscal.backend.service.ApiConnService;
import com.efiscal.backend.service.ApiConnService.ApiConnDto;
import com.efiscal.backend.service.ApiConnService.ApiConnRequest;
import com.efiscal.backend.service.ApiConnService.ApiTemplateDto;
import com.efiscal.backend.service.ApiConnService.ApiTemplateRequest;
import com.efiscal.backend.service.DemoDataService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ApiConfigController {

    private final ApiConnService apiConnService;

    public ApiConfigController(ApiConnService apiConnService) {
        this.apiConnService = apiConnService;
    }

    @GetMapping("/apiconn")
    public List<ApiConnDto> listConnections(@RequestParam(required = false) Long orgId) {
        return apiConnService.listConnections(orgId);
    }

    @PostMapping("/apiconn")
    public ResponseEntity<?> createConnection(@RequestBody ApiConnRequest req) {
        if (!isSuperAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        return ResponseEntity.status(HttpStatus.CREATED).body(apiConnService.createConnection(req));
    }

    @PutMapping("/apiconn/{id}")
    public ResponseEntity<?> updateConnection(@PathVariable Long id, @RequestBody ApiConnRequest req) {
        if (!isSuperAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        return ResponseEntity.ok(apiConnService.updateConnection(id, req));
    }

    @GetMapping("/apitemplate")
    public List<ApiTemplateDto> listTemplates(@RequestParam Long apiconnId) {
        return apiConnService.listTemplates(apiconnId);
    }

    @PostMapping("/apitemplate")
    public ResponseEntity<?> createTemplate(@RequestBody ApiTemplateRequest req) {
        if (!isSuperAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        return ResponseEntity.status(HttpStatus.CREATED).body(apiConnService.createTemplate(req));
    }

    @PutMapping("/apitemplate/{id}")
    public ResponseEntity<?> updateTemplate(@PathVariable Long id, @RequestBody ApiTemplateRequest req) {
        if (!isSuperAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        return ResponseEntity.ok(apiConnService.updateTemplate(id, req));
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

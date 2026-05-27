package com.efiscal.backend.controller;

import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.ApiConnService;
import com.efiscal.backend.service.ApiConnService.ApiConnDto;
import com.efiscal.backend.service.ApiConnService.ApiConnRequest;
import com.efiscal.backend.service.ApiConnService.ApiTemplateDto;
import com.efiscal.backend.service.ApiConnService.ApiTemplateRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ApiConfigController {

    private final ApiConnService apiConnService;
    private final AuthorizationService authorizationService;

    public ApiConfigController(ApiConnService apiConnService, AuthorizationService authorizationService) {
        this.apiConnService = apiConnService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/apiconn")
    public List<ApiConnDto> listConnections(@RequestParam(required = false) Long orgId) {
        authorizationService.requireAction("ORGS_MANAGE");
        return apiConnService.listConnections(
            orgId,
            authorizationService.getClientId(),
            authorizationService.isSuperAdmin()
        );
    }

    @PostMapping("/apiconn")
    public ResponseEntity<?> createConnection(@RequestBody ApiConnRequest req) {
        authorizationService.requireAction("ORGS_MANAGE");
        return ResponseEntity.status(HttpStatus.CREATED).body(
            apiConnService.createConnection(
                req,
                authorizationService.getClientId(),
                authorizationService.isSuperAdmin()
            )
        );
    }

    @PutMapping("/apiconn/{id}")
    public ResponseEntity<?> updateConnection(@PathVariable Long id, @RequestBody ApiConnRequest req) {
        authorizationService.requireAction("ORGS_MANAGE");
        return ResponseEntity.ok(
            apiConnService.updateConnection(
                id,
                req,
                authorizationService.getClientId(),
                authorizationService.isSuperAdmin()
            )
        );
    }

    @GetMapping("/apitemplate")
    public List<ApiTemplateDto> listTemplates(@RequestParam Long apiconnId) {
        authorizationService.requireAction("ORGS_MANAGE");
        return apiConnService.listTemplates(
            apiconnId,
            authorizationService.getClientId(),
            authorizationService.isSuperAdmin()
        );
    }

    @PostMapping("/apitemplate")
    public ResponseEntity<?> createTemplate(@RequestBody ApiTemplateRequest req) {
        authorizationService.requireAction("ORGS_MANAGE");
        return ResponseEntity.status(HttpStatus.CREATED).body(
            apiConnService.createTemplate(
                req,
                authorizationService.getClientId(),
                authorizationService.isSuperAdmin()
            )
        );
    }

    @PutMapping("/apitemplate/{id}")
    public ResponseEntity<?> updateTemplate(@PathVariable Long id, @RequestBody ApiTemplateRequest req) {
        authorizationService.requireAction("ORGS_MANAGE");
        return ResponseEntity.ok(
            apiConnService.updateTemplate(
                id,
                req,
                authorizationService.getClientId(),
                authorizationService.isSuperAdmin()
            )
        );
    }
}

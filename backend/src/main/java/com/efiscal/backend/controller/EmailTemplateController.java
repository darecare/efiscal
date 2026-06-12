package com.efiscal.backend.controller;

import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.EmailTemplateService;
import com.efiscal.backend.service.EmailTemplateService.CreateEmailTemplateRequest;
import com.efiscal.backend.service.EmailTemplateService.EmailTemplateDto;
import com.efiscal.backend.service.EmailTemplateService.UpdateEmailTemplateRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email-templates")
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;
    private final AuthorizationService authorizationService;

    public EmailTemplateController(EmailTemplateService emailTemplateService,
                                   AuthorizationService authorizationService) {
        this.emailTemplateService = emailTemplateService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<EmailTemplateDto> listTemplates(@RequestParam Long orgId) {
        authorizationService.requireAction("ORGS_MANAGE");
        return emailTemplateService.listTemplates(
            orgId,
            authorizationService.getClientId(),
            authorizationService.isSuperAdmin()
        );
    }

    @PostMapping
    public ResponseEntity<?> createTemplate(@RequestBody CreateEmailTemplateRequest req) {
        authorizationService.requireAction("ORGS_MANAGE");
        return ResponseEntity.status(HttpStatus.CREATED).body(
            emailTemplateService.createTemplate(
                req,
                authorizationService.getClientId(),
                authorizationService.isSuperAdmin()
            )
        );
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<?> updateTemplate(@PathVariable Long templateId, @RequestBody UpdateEmailTemplateRequest req) {
        authorizationService.requireAction("ORGS_MANAGE");
        return ResponseEntity.ok(
            emailTemplateService.updateTemplate(
                templateId,
                req,
                authorizationService.getClientId(),
                authorizationService.isSuperAdmin()
            )
        );
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long templateId) {
        authorizationService.requireAction("ORGS_MANAGE");
        emailTemplateService.deleteTemplate(
            templateId,
            authorizationService.getClientId(),
            authorizationService.isSuperAdmin()
        );
        return ResponseEntity.noContent().build();
    }
}
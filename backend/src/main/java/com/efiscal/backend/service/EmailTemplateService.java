package com.efiscal.backend.service;

import com.efiscal.backend.model.EmailTemplateEntity;
import com.efiscal.backend.model.OrgEntity;
import com.efiscal.backend.repository.EmailTemplateRepository;
import com.efiscal.backend.repository.OrgRepository;
import com.efiscal.backend.security.AuthorizationService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final OrgRepository orgRepository;
    private final AuthorizationService authorizationService;

    public EmailTemplateService(EmailTemplateRepository emailTemplateRepository,
                                OrgRepository orgRepository,
                                AuthorizationService authorizationService) {
        this.emailTemplateRepository = emailTemplateRepository;
        this.orgRepository = orgRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<EmailTemplateDto> listTemplates(Long orgId, Long callerClientId, boolean isSuperAdmin) {
        if (orgId != null) {
            authorizationService.requireOrgAccess(orgId);
            return emailTemplateRepository.findAllByOrgOrgIdAndDeletedAtIsNull(orgId).stream().map(this::toDto).toList();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orgId is required");
    }

    @Transactional
    public EmailTemplateDto createTemplate(CreateEmailTemplateRequest req, Long callerClientId, boolean isSuperAdmin) {
        validateRequest(req.orgId(), req.templateName(), req.subject(), req.bodyHtml());
        authorizationService.requireOrgAccess(req.orgId());

        OrgEntity org = orgRepository.findById(req.orgId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Org not found"));

        EmailTemplateEntity entity = new EmailTemplateEntity();
        entity.setOrg(org);
        entity.setTemplateName(req.templateName().trim());
        entity.setSubject(req.subject().trim());
        entity.setBodyHtml(req.bodyHtml());
        entity.setActive(req.isActive() != null ? req.isActive() : true);
        return toDto(emailTemplateRepository.save(entity));
    }

    @Transactional
    public EmailTemplateDto updateTemplate(Long templateId, UpdateEmailTemplateRequest req, Long callerClientId, boolean isSuperAdmin) {
        EmailTemplateEntity entity = emailTemplateRepository.findById(templateId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email template not found"));

        authorizationService.requireOrgAccess(entity.getOrg().getOrgId());

        if (req.templateName() != null && req.templateName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template name cannot be blank");
        }
        if (req.subject() != null && req.subject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject cannot be blank");
        }
        if (req.bodyHtml() != null && req.bodyHtml().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body HTML cannot be blank");
        }

        if (req.orgId() != null) {
            authorizationService.requireOrgAccess(req.orgId());
            OrgEntity org = orgRepository.findById(req.orgId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Org not found"));
            entity.setOrg(org);
        }
        if (req.templateName() != null) entity.setTemplateName(req.templateName().trim());
        if (req.subject() != null) entity.setSubject(req.subject().trim());
        if (req.bodyHtml() != null) entity.setBodyHtml(req.bodyHtml());
        if (req.isActive() != null) entity.setActive(req.isActive());
        return toDto(emailTemplateRepository.save(entity));
    }

    @Transactional
    public void deleteTemplate(Long templateId, Long callerClientId, boolean isSuperAdmin) {
        EmailTemplateEntity entity = emailTemplateRepository.findById(templateId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email template not found"));
        authorizationService.requireOrgAccess(entity.getOrg().getOrgId());
        entity.setDeletedAt(OffsetDateTime.now());
        emailTemplateRepository.save(entity);
    }

    private void validateRequest(Long orgId, String templateName, String subject, String bodyHtml) {
        if (orgId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orgId is required");
        }
        if (templateName == null || templateName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template name is required");
        }
        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject is required");
        }
        if (bodyHtml == null || bodyHtml.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body HTML is required");
        }
    }

    private EmailTemplateDto toDto(EmailTemplateEntity entity) {
        return new EmailTemplateDto(
            entity.getEmailTemplateId(),
            entity.getOrg().getOrgId(),
            entity.getOrg().getName(),
            entity.getTemplateName(),
            entity.getSubject(),
            entity.getBodyHtml(),
            entity.isActive(),
            entity.getCreatedAt()
        );
    }

    public record EmailTemplateDto(
        Long emailTemplateId,
        Long orgId,
        String orgName,
        String templateName,
        String subject,
        String bodyHtml,
        boolean isActive,
        OffsetDateTime createdAt
    ) {}

    public record CreateEmailTemplateRequest(
        @NotNull(message = "Org ID is required")
        Long orgId,

        @Size(max = 120, message = "Template name must not exceed 120 characters")
        String templateName,

        @Size(max = 255, message = "Subject must not exceed 255 characters")
        String subject,

        String bodyHtml,

        Boolean isActive
    ) {}

    public record UpdateEmailTemplateRequest(
        Long orgId,

        @Size(max = 120, message = "Template name must not exceed 120 characters")
        String templateName,

        @Size(max = 255, message = "Subject must not exceed 255 characters")
        String subject,

        String bodyHtml,

        Boolean isActive
    ) {}
}
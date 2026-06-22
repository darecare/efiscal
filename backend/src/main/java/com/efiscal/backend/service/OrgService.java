package com.efiscal.backend.service;

import com.efiscal.backend.model.ClientEntity;
import com.efiscal.backend.model.OrgEntity;
import com.efiscal.backend.model.OrgPayTypeEntity;
import com.efiscal.backend.repository.AppUserRepository;
import com.efiscal.backend.repository.ClientRepository;
import com.efiscal.backend.repository.OrgPayTypeRepository;
import com.efiscal.backend.repository.OrgRepository;
import com.efiscal.backend.repository.UserOrgAccessRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrgService {

    private static final Set<String> ALLOWED_SMTP_CONNECTION_SECURITY = Set.of("STARTTLS", "SSL_TLS");

    private final OrgRepository orgRepository;
    private final ClientRepository clientRepository;
    private final AppUserRepository appUserRepository;
    private final UserOrgAccessRepository userOrgAccessRepository;
    private final OrgPayTypeRepository orgPayTypeRepository;

    public OrgService(OrgRepository orgRepository, ClientRepository clientRepository,
                      AppUserRepository appUserRepository, UserOrgAccessRepository userOrgAccessRepository,
                      OrgPayTypeRepository orgPayTypeRepository) {
        this.orgRepository = orgRepository;
        this.clientRepository = clientRepository;
        this.appUserRepository = appUserRepository;
        this.userOrgAccessRepository = userOrgAccessRepository;
        this.orgPayTypeRepository = orgPayTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<OrgDto> listOrgs(Long clientId, Long callerClientId, boolean isSuperAdmin) {
        if (!isSuperAdmin) {
            if (callerClientId == null) {
                return List.of();
            }
            return orgRepository.findAllByClientClientIdAndDeletedAtIsNull(callerClientId)
                .stream().map(this::toDto).toList();
        }
        List<OrgEntity> entities = clientId != null
            ? orgRepository.findAllByClientClientIdAndDeletedAtIsNull(clientId)
            : orgRepository.findAllByDeletedAtIsNull();
        return entities.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrgDto getOrg(Long orgId, Long callerClientId, boolean isSuperAdmin) {
        OrgEntity org = orgRepository.findById(orgId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        if (!isSuperAdmin && !org.getClient().getClientId().equals(callerClientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return toDto(org);
    }

    @Transactional
    public OrgDto createOrg(CreateOrgRequest req, Long callerClientId, boolean isSuperAdmin) {
        if (!isSuperAdmin && (req.clientId() == null || !req.clientId().equals(callerClientId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (req.clientId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client ID must not be null");
        }
        ClientEntity client = clientRepository.findById(req.clientId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));

        OrgEntity org = new OrgEntity();
        org.setClient(client);
        org.setName(req.name());
        org.setTaxId(req.taxId());
        org.setStatus(req.status() != null ? req.status() : "ACTIVE");
        org.setCurrency(req.currency() != null ? req.currency() : "RSD");
        org.setActive(req.isActive() != null ? req.isActive() : true);
        org.setSmtpServer(normalizeOptional(req.smtpServer()));
        org.setSmtpPort(req.smtpPort());
        org.setEmailFrom(normalizeOptional(req.emailFrom()));
        org.setSmtpUsername(normalizeOptional(req.smtpUsername()));
        org.setSmtpPassword(normalizeOptional(req.smtpPassword()));
        org.setSmtpConnectionSecurity(validateAndNormalizeSmtpConnectionSecurity(req.smtpConnectionSecurity()));
        return toDto(orgRepository.save(org));
    }

    @Transactional
    public OrgDto updateOrg(Long orgId, UpdateOrgRequest req, Long callerClientId, boolean isSuperAdmin) {
        OrgEntity org = orgRepository.findById(orgId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        if (!isSuperAdmin && !org.getClient().getClientId().equals(callerClientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (req.clientId() != null) {
            if (!isSuperAdmin && !req.clientId().equals(callerClientId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot assign organization to another client");
            }
            ClientEntity client = clientRepository.findById(req.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));
            org.setClient(client);
        }
        if (req.name() != null) org.setName(req.name());
        if (req.taxId() != null) org.setTaxId(req.taxId());
        if (req.status() != null) org.setStatus(req.status());
        if (req.currency() != null) org.setCurrency(req.currency());
        if (req.isActive() != null) org.setActive(req.isActive());
        if (req.smtpServer() != null) org.setSmtpServer(normalizeOptional(req.smtpServer()));
        if (req.smtpPort() != null) org.setSmtpPort(req.smtpPort());
        if (req.emailFrom() != null) org.setEmailFrom(normalizeOptional(req.emailFrom()));
        if (req.smtpUsername() != null) org.setSmtpUsername(normalizeOptional(req.smtpUsername()));
        if (req.smtpPassword() != null) org.setSmtpPassword(normalizeOptional(req.smtpPassword()));
        if (req.smtpConnectionSecurity() != null) {
            org.setSmtpConnectionSecurity(validateAndNormalizeSmtpConnectionSecurity(req.smtpConnectionSecurity()));
        }
        if (req.advertisementHtml() != null) org.setAdvertisementHtml(normalizeOptional(req.advertisementHtml()));
        if (req.advertisementEnabled() != null) org.setAdvertisementEnabled(req.advertisementEnabled());
        return toDto(orgRepository.save(org));
    }

    @Transactional(readOnly = true)
    public List<OrgDto> listMyOrgs(String email, boolean isSuperAdmin) {
        if (isSuperAdmin) {
            return orgRepository.findAllByDeletedAtIsNull().stream().map(this::toDto).toList();
        }
        return appUserRepository.findByEmail(email)
            .map(user -> userOrgAccessRepository.findAllByIdUserId(user.getUserId())
                .stream()
                .map(access -> orgRepository.findById(access.getId().getOrgId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(o -> o.getDeletedAt() == null)
                .map(this::toDto)
                .toList())
            .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<Integer> getOrgPaymentTypes(Long orgId) {
        orgRepository.findById(orgId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        return orgPayTypeRepository.findByOrgId(orgId).stream()
            .map(OrgPayTypeEntity::getPaymentType)
            .toList();
    }

    @Transactional
    public void setOrgPaymentTypes(Long orgId, List<Integer> paymentTypes) {
        orgRepository.findById(orgId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        
        orgPayTypeRepository.deleteByOrgId(orgId);
        
        List<OrgPayTypeEntity> entities = paymentTypes.stream()
            .distinct()
            .map(pt -> new OrgPayTypeEntity(orgId, pt))
            .toList();
        
        orgPayTypeRepository.saveAll(entities);
    }

    private OrgDto toDto(OrgEntity o) {
        return new OrgDto(
            o.getOrgId(),
            o.getClient().getClientId(),
            o.getClient().getName(),
            o.getName(),
            o.getTaxId(),
            o.getStatus(),
            o.getCurrency(),
            o.isActive(),
            o.getSmtpServer(),
            o.getSmtpPort(),
            o.getEmailFrom(),
            o.getSmtpUsername(),
            o.getSmtpConnectionSecurity(),
            o.getCreatedAt(),
            o.getAdvertisementHtml(),
            o.isAdvertisementEnabled()
        );
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String validateAndNormalizeSmtpConnectionSecurity(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_SMTP_CONNECTION_SECURITY.contains(upper)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "SMTP connection security must be STARTTLS or SSL_TLS"
            );
        }
        return upper;
    }

    public record OrgDto(
        Long orgId,
        Long clientId,
        String clientName,
        String name,
        String taxId,
        String status,
        String currency,
        boolean isActive,
        String smtpServer,
        @Min(value = 1, message = "SMTP port must be at least 1")
        @Max(value = 65535, message = "SMTP port must be at most 65535")
        Integer smtpPort,
        String emailFrom,
        String smtpUsername,
        String smtpConnectionSecurity,
        OffsetDateTime createdAt,
        String advertisementHtml,
        boolean advertisementEnabled
    ) {}

    public record CreateOrgRequest(
        @NotNull(message = "Client ID is required")
        Long clientId,

        @NotBlank(message = "Organization name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Tax ID is required")
        @Size(max = 50, message = "Tax ID must not exceed 50 characters")
        String taxId,

        @Size(max = 30, message = "Status must not exceed 30 characters")
        String status,

        @Size(max = 10, message = "Currency must not exceed 10 characters")
        String currency,

        Boolean isActive,

        @Size(max = 255, message = "SMTP server must not exceed 255 characters")
        String smtpServer,

        @Min(value = 1, message = "SMTP port must be at least 1")
        @Max(value = 65535, message = "SMTP port must be at most 65535")
        Integer smtpPort,

        @Size(max = 255, message = "From email must not exceed 255 characters")
        String emailFrom,

        @Size(max = 255, message = "SMTP username must not exceed 255 characters")
        String smtpUsername,

        @Size(max = 255, message = "SMTP password must not exceed 255 characters")
        String smtpPassword,

        @Size(max = 20, message = "SMTP connection security must not exceed 20 characters")
        String smtpConnectionSecurity
    ) {}

    public record UpdateOrgRequest(
        Long clientId,

        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Size(max = 50, message = "Tax ID must not exceed 50 characters")
        String taxId,

        @Size(max = 30, message = "Status must not exceed 30 characters")
        String status,

        @Size(max = 10, message = "Currency must not exceed 10 characters")
        String currency,

        Boolean isActive,

        @Size(max = 255, message = "SMTP server must not exceed 255 characters")
        String smtpServer,

        Integer smtpPort,

        @Size(max = 255, message = "From email must not exceed 255 characters")
        String emailFrom,

        @Size(max = 255, message = "SMTP username must not exceed 255 characters")
        String smtpUsername,

        @Size(max = 255, message = "SMTP password must not exceed 255 characters")
        String smtpPassword,

        @Size(max = 20, message = "SMTP connection security must not exceed 20 characters")
        String smtpConnectionSecurity,

        String advertisementHtml,
        Boolean advertisementEnabled
    ) {}
}

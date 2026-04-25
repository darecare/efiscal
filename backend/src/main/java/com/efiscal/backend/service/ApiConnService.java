package com.efiscal.backend.service;

import com.efiscal.backend.model.ApiConnEntity;
import com.efiscal.backend.model.ApiTemplateEntity;
import com.efiscal.backend.model.OrgEntity;
import com.efiscal.backend.repository.ApiConnRepository;
import com.efiscal.backend.repository.ApiTemplateRepository;
import com.efiscal.backend.repository.OrgRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApiConnService {

    private final ApiConnRepository apiConnRepository;
    private final ApiTemplateRepository apiTemplateRepository;
    private final OrgRepository orgRepository;

    public ApiConnService(ApiConnRepository apiConnRepository,
                          ApiTemplateRepository apiTemplateRepository,
                          OrgRepository orgRepository) {
        this.apiConnRepository = apiConnRepository;
        this.apiTemplateRepository = apiTemplateRepository;
        this.orgRepository = orgRepository;
    }

    @Transactional(readOnly = true)
    public List<ApiConnDto> listConnections(Long orgId) {
        List<ApiConnEntity> list = orgId != null
            ? apiConnRepository.findAllByOrgOrgIdAndDeletedAtIsNull(orgId)
            : apiConnRepository.findAllByDeletedAtIsNull();
        return list.stream().map(this::toConnDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ApiTemplateDto> listTemplates(Long apiconnId) {
        return apiTemplateRepository.findAllByApiConnApiconnId(apiconnId)
            .stream().map(this::toTemplateDto).toList();
    }

    @Transactional
    public ApiConnDto createConnection(ApiConnRequest req) {
        OrgEntity org = orgRepository.findById(req.orgId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Org not found"));
        ApiConnEntity e = new ApiConnEntity();
        e.setOrg(org);
        e.setDisplayName(req.displayName());
        e.setApiPlatform(req.apiPlatform());
        e.setApiBaseUrl(req.apiBaseUrl());
        e.setApiauthtype(req.apiauthtype());
        e.setApikey(req.apikey());
        e.setApisecret(req.apisecret());
        e.setActive(req.isActive() != null ? req.isActive() : true);
        return toConnDto(apiConnRepository.save(e));
    }

    @Transactional
    public ApiConnDto updateConnection(Long apiconnId, ApiConnRequest req) {
        ApiConnEntity e = apiConnRepository.findById(apiconnId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found"));
        if (req.orgId() != null) {
            OrgEntity org = orgRepository.findById(req.orgId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Org not found"));
            e.setOrg(org);
        }
        if (req.displayName() != null) e.setDisplayName(req.displayName());
        if (req.apiPlatform() != null) e.setApiPlatform(req.apiPlatform());
        if (req.apiBaseUrl() != null) e.setApiBaseUrl(req.apiBaseUrl());
        if (req.apiauthtype() != null) e.setApiauthtype(req.apiauthtype());
        if (req.apikey() != null) e.setApikey(req.apikey());
        if (req.apisecret() != null) e.setApisecret(req.apisecret());
        if (req.isActive() != null) e.setActive(req.isActive());
        return toConnDto(apiConnRepository.save(e));
    }

    @Transactional
    public ApiTemplateDto createTemplate(ApiTemplateRequest req) {
        ApiConnEntity conn = apiConnRepository.findById(req.apiconnId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Connection not found"));
        ApiTemplateEntity t = new ApiTemplateEntity();
        t.setApiConn(conn);
        t.setOperationKey(req.operationKey());
        t.setHttpMethod(req.httpMethod());
        t.setContentType(req.contentType() != null ? req.contentType() : "application/json");
        t.setEndpointPath(req.endpointPath());
        t.setActive(req.isActive() != null ? req.isActive() : true);
        return toTemplateDto(apiTemplateRepository.save(t));
    }

    @Transactional
    public ApiTemplateDto updateTemplate(Long apitemplateId, ApiTemplateRequest req) {
        ApiTemplateEntity t = apiTemplateRepository.findById(apitemplateId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
        if (req.operationKey() != null) t.setOperationKey(req.operationKey());
        if (req.httpMethod() != null) t.setHttpMethod(req.httpMethod());
        if (req.contentType() != null) t.setContentType(req.contentType());
        if (req.endpointPath() != null) t.setEndpointPath(req.endpointPath());
        if (req.isActive() != null) t.setActive(req.isActive());
        return toTemplateDto(apiTemplateRepository.save(t));
    }

    private ApiConnDto toConnDto(ApiConnEntity e) {
        return new ApiConnDto(e.getApiconnId(), e.getOrg().getOrgId(), e.getOrg().getName(),
            e.getDisplayName(), e.getApiPlatform(), e.getApiBaseUrl(),
            e.getApiauthtype(), e.isActive(), e.getCreatedAt());
    }

    private ApiTemplateDto toTemplateDto(ApiTemplateEntity t) {
        return new ApiTemplateDto(t.getApitemplateId(), t.getApiConn().getApiconnId(),
            t.getOperationKey(), t.getHttpMethod(), t.getContentType(),
            t.getEndpointPath(), t.isActive(), t.getCreatedAt());
    }

    public record ApiConnDto(Long apiconnId, Long orgId, String orgName, String displayName,
        String apiPlatform, String apiBaseUrl, String apiauthtype,
        boolean isActive, OffsetDateTime createdAt) {}

    public record ApiTemplateDto(Long apitemplateId, Long apiconnId, String operationKey,
        String httpMethod, String contentType, String endpointPath,
        boolean isActive, OffsetDateTime createdAt) {}

    public record ApiConnRequest(Long orgId, String displayName, String apiPlatform,
        String apiBaseUrl, String apiauthtype, String apikey, String apisecret, Boolean isActive) {}

    public record ApiTemplateRequest(Long apiconnId, String operationKey, String httpMethod,
        String contentType, String endpointPath, Boolean isActive) {}
}

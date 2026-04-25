package com.efiscal.backend.service;

import com.efiscal.backend.model.ClientEntity;
import com.efiscal.backend.model.OrgEntity;
import com.efiscal.backend.repository.ClientRepository;
import com.efiscal.backend.repository.OrgRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;



@Service
public class OrgService {

    private final OrgRepository orgRepository;
    private final ClientRepository clientRepository;

    public OrgService(OrgRepository orgRepository, ClientRepository clientRepository) {
        this.orgRepository = orgRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<OrgDto> listOrgs(Long clientId) {
        List<OrgEntity> entities = clientId != null
            ? orgRepository.findAllByClientClientIdAndDeletedAtIsNull(clientId)
            : orgRepository.findAllByDeletedAtIsNull();
        return entities.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrgDto getOrg(Long orgId) {
        return orgRepository.findById(orgId)
            .map(this::toDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    @Transactional
    public OrgDto createOrg(OrgRequest req) {
        ClientEntity client = clientRepository.findById(req.clientId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));

        OrgEntity org = new OrgEntity();
        org.setClient(client);
        org.setName(req.name());
        org.setTaxId(req.taxId());
        org.setStatus(req.status() != null ? req.status() : "ACTIVE");
        org.setCurrency(req.currency() != null ? req.currency() : "RSD");
        org.setActive(req.isActive() != null ? req.isActive() : true);
        return toDto(orgRepository.save(org));
    }

    @Transactional
    public OrgDto updateOrg(Long orgId, OrgRequest req) {
        OrgEntity org = orgRepository.findById(orgId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        if (req.clientId() != null) {
            ClientEntity client = clientRepository.findById(req.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));
            org.setClient(client);
        }
        if (req.name() != null) org.setName(req.name());
        if (req.taxId() != null) org.setTaxId(req.taxId());
        if (req.status() != null) org.setStatus(req.status());
        if (req.currency() != null) org.setCurrency(req.currency());
        if (req.isActive() != null) org.setActive(req.isActive());
        return toDto(orgRepository.save(org));
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
            o.getCreatedAt()
        );
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
        OffsetDateTime createdAt
    ) {}

    public record OrgRequest(
        Long clientId,
        String name,
        String taxId,
        String status,
        String currency,
        Boolean isActive
    ) {}
}

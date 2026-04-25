package com.efiscal.backend.service;

import com.efiscal.backend.model.ClientEntity;
import com.efiscal.backend.repository.ClientRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<ClientDto> listClients() {
        return clientRepository.findAllByDeletedAtIsNull().stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public ClientDto getClient(Long clientId) {
        return clientRepository.findById(clientId)
            .map(this::toDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
    }

    @Transactional
    public ClientDto createClient(ClientRequest req) {
        ClientEntity client = new ClientEntity();
        client.setName(req.name());
        client.setStatus(req.status() != null ? req.status() : "ACTIVE");
        client.setCurrency(req.currency() != null ? req.currency() : "RSD");
        client.setActive(req.isActive() != null ? req.isActive() : true);
        return toDto(clientRepository.save(client));
    }

    @Transactional
    public ClientDto updateClient(Long clientId, ClientRequest req) {
        ClientEntity client = clientRepository.findById(clientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        if (req.name() != null) client.setName(req.name());
        if (req.status() != null) client.setStatus(req.status());
        if (req.currency() != null) client.setCurrency(req.currency());
        if (req.isActive() != null) client.setActive(req.isActive());
        return toDto(clientRepository.save(client));
    }

    private ClientDto toDto(ClientEntity c) {
        return new ClientDto(
            c.getClientId(),
            c.getName(),
            c.getStatus(),
            c.getCurrency(),
            c.isActive(),
            c.getCreatedAt()
        );
    }

    public record ClientDto(
        Long clientId,
        String name,
        String status,
        String currency,
        boolean isActive,
        OffsetDateTime createdAt
    ) {}

    public record ClientRequest(
        String name,
        String status,
        String currency,
        Boolean isActive
    ) {}
}

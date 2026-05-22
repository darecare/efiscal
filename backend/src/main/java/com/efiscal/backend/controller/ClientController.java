package com.efiscal.backend.controller;

import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.ClientService;
import com.efiscal.backend.service.ClientService.ClientDto;
import com.efiscal.backend.service.ClientService.ClientRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService clientService;
    private final AuthorizationService authorizationService;

    public ClientController(ClientService clientService, AuthorizationService authorizationService) {
        this.clientService = clientService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<ClientDto> listClients() {
        authorizationService.requireSuperAdmin();
        return clientService.listClients();
    }

    @GetMapping("/{clientId}")
    public ClientDto getClient(@PathVariable Long clientId) {
        if (!authorizationService.isSuperAdmin()) {
            Long callerClientId = authorizationService.getClientId();
            if (callerClientId == null || !callerClientId.equals(clientId)) {
                authorizationService.requireSuperAdmin();
            }
        }
        return clientService.getClient(clientId);
    }

    @PostMapping
    public ResponseEntity<?> createClient(@RequestBody ClientRequest req) {
        authorizationService.requireSuperAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(req));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<?> updateClient(@PathVariable Long clientId, @RequestBody ClientRequest req) {
        authorizationService.requireSuperAdmin();
        return ResponseEntity.ok(clientService.updateClient(clientId, req));
    }
}

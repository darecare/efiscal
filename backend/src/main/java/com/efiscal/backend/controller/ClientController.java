package com.efiscal.backend.controller;

import com.efiscal.backend.service.ClientService;
import com.efiscal.backend.service.ClientService.ClientDto;
import com.efiscal.backend.service.ClientService.ClientRequest;
import com.efiscal.backend.service.DemoDataService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public List<ClientDto> listClients() {
        return clientService.listClients();
    }

    @GetMapping("/{clientId}")
    public ClientDto getClient(@PathVariable Long clientId) {
        return clientService.getClient(clientId);
    }

    @PostMapping
    public ResponseEntity<?> createClient(@RequestBody ClientRequest req) {
        if (!isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(req));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<?> updateClient(@PathVariable Long clientId, @RequestBody ClientRequest req) {
        if (!isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        }
        return ResponseEntity.ok(clientService.updateClient(clientId, req));
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

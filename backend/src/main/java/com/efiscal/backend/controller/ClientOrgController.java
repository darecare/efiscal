package com.efiscal.backend.controller;

import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.DemoDataService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients-orgs")
public class ClientOrgController {

    private final DemoDataService demoDataService;
    private final AuthorizationService authorizationService;

    public ClientOrgController(DemoDataService demoDataService, AuthorizationService authorizationService) {
        this.demoDataService = demoDataService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<DemoDataService.ClientOrgView> listClientOrgs() {
        authorizationService.requireAction("ORGS_MANAGE");
        List<DemoDataService.ClientOrgView> all = demoDataService.listClientOrgs();
        if (authorizationService.isSuperAdmin()) {
            return all;
        }
        String clientName = authorizationService.getCurrentUser()
            .map(DemoDataService.AuthenticatedUser::clientName)
            .orElse("");
        return all.stream()
            .filter(org -> org.clientName().equalsIgnoreCase(clientName))
            .toList();
    }
}

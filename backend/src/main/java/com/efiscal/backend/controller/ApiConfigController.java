package com.efiscal.backend.controller;

import com.efiscal.backend.service.DemoDataService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/api-config")
public class ApiConfigController {

    private final DemoDataService demoDataService;

    public ApiConfigController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping("/connections")
    public List<DemoDataService.ApiConnectionView> listConnections() {
        return demoDataService.listApiConnections();
    }

    @GetMapping("/templates")
    public List<DemoDataService.ApiTemplateView> listTemplates() {
        return demoDataService.listApiTemplates();
    }
}

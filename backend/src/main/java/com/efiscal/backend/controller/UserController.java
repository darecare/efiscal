package com.efiscal.backend.controller;

import com.efiscal.backend.service.DemoDataService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final DemoDataService demoDataService;

    public UserController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping
    public List<DemoDataService.AuthenticatedUser> listUsers() {
        return demoDataService.listUsers();
    }
}

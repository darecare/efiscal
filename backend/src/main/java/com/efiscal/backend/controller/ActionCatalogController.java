package com.efiscal.backend.controller;

import com.efiscal.backend.model.ActionCatalogEntity;
import com.efiscal.backend.repository.ActionCatalogRepository;
import com.efiscal.backend.security.AuthorizationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/actions")
public class ActionCatalogController {

    private final ActionCatalogRepository actionCatalogRepository;
    private final AuthorizationService authorizationService;

    public ActionCatalogController(
        ActionCatalogRepository actionCatalogRepository,
        AuthorizationService authorizationService
    ) {
        this.actionCatalogRepository = actionCatalogRepository;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<ActionDto> listActions(@RequestParam(required = false) String module) {
        authorizationService.requireAnyAction("ROLES_MANAGE", "USERS_MANAGE");
        List<ActionCatalogEntity> actions = module != null && !module.isBlank()
            ? actionCatalogRepository.findByModuleCodeAndIsActiveTrue(module.trim())
            : actionCatalogRepository.findAll();
        return actions.stream()
            .filter(ActionCatalogEntity::isActive)
            .map(a -> new ActionDto(a.getActionId(), a.getModuleCode(), a.getActionCode(), a.getName(), a.getDescription()))
            .toList();
    }

    public record ActionDto(Long actionId, String moduleCode, String actionCode, String name, String description) {}
}

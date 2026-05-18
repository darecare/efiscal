package com.efiscal.backend.controller;

import com.efiscal.backend.model.ActionCatalogEntity;
import com.efiscal.backend.repository.ActionCatalogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/actions")
public class ActionCatalogController {

    private final ActionCatalogRepository actionCatalogRepository;

    public ActionCatalogController(ActionCatalogRepository actionCatalogRepository) {
        this.actionCatalogRepository = actionCatalogRepository;
    }

    @GetMapping
    public List<ActionDto> listActions() {
        return actionCatalogRepository.findAll().stream()
                .filter(ActionCatalogEntity::isActive)
                .map(a -> new ActionDto(a.getActionId(), a.getModuleCode(), a.getActionCode(), a.getName(), a.getDescription()))
                .toList();
    }

    public record ActionDto(Long actionId, String moduleCode, String actionCode, String name, String description) {}
}

package com.efiscal.backend.controller;

import com.efiscal.backend.model.RoleEntity;
import com.efiscal.backend.repository.RoleRepository;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public List<Map<String, Object>> listRoles() {
        return roleRepository.findAll().stream()
            .filter(RoleEntity::isActive)
            .map(r -> Map.<String, Object>of(
                "roleId", r.getRoleId(),
                "roleCode", r.getRoleCode(),
                "name", r.getName()
            ))
            .toList();
    }
}

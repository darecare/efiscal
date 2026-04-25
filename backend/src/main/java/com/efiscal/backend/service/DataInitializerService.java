package com.efiscal.backend.service;

import com.efiscal.backend.model.AppUserEntity;
import com.efiscal.backend.model.ClientEntity;
import com.efiscal.backend.model.RoleEntity;
import com.efiscal.backend.repository.AppUserRepository;
import com.efiscal.backend.repository.ClientRepository;
import com.efiscal.backend.repository.RoleRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds minimum required data on first startup.
 * Idempotent: checks for existence before inserting.
 */
@Component
public class DataInitializerService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerService.class);

    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;
    private final AppUserRepository appUserRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataInitializerService(
        RoleRepository roleRepository,
        ClientRepository clientRepository,
        AppUserRepository appUserRepository
    ) {
        this.roleRepository = roleRepository;
        this.clientRepository = clientRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    @Transactional
    public void run(String... args) {
        RoleEntity superAdminRole = seedRole("SUPERADMIN", "Super Administrator",
            "Full unrestricted access across all clients and organizations");

        seedRole("CLIENT_ADMIN", "Client Administrator",
            "Administrative access within an assigned client scope");

        seedRole("OPERATOR", "Operator",
            "Standard operational access for day-to-day tasks");

        ClientEntity globalClient = seedClient("Global", "ACTIVE", "RSD");

        seedAdminUser(globalClient, superAdminRole);
    }

    private RoleEntity seedRole(String roleCode, String name, String description) {
        return roleRepository.findByRoleCode(roleCode).orElseGet(() -> {
            RoleEntity role = new RoleEntity();
            role.setRoleCode(roleCode);
            role.setName(name);
            role.setDescription(description);
            role.setActive(true);
            RoleEntity saved = roleRepository.save(role);
            log.info("Seeded role: {}", roleCode);
            return saved;
        });
    }

    private ClientEntity seedClient(String name, String status, String currency) {
        return clientRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            ClientEntity client = new ClientEntity();
            client.setName(name);
            client.setStatus(status);
            client.setCurrency(currency);
            client.setActive(true);
            ClientEntity saved = clientRepository.save(client);
            log.info("Seeded client: {}", name);
            return saved;
        });
    }

    private void seedAdminUser(ClientEntity client, RoleEntity role) {
        if (!appUserRepository.existsByEmail("admin@efiscal.local")) {
            AppUserEntity admin = new AppUserEntity();
            admin.setEmail("admin@efiscal.local");
            admin.setPasswordHash(passwordEncoder.encode("Admin123!"));
            admin.setFullName("System Superadmin");
            admin.setClient(client);
            admin.setRole(role);
            admin.setSubscriptionStatus("ACTIVE");
            admin.setActive(true);
            appUserRepository.save(admin);
            log.info("Seeded admin user: admin@efiscal.local");
        }
    }
}

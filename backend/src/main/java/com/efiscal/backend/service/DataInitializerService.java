package com.efiscal.backend.service;

import com.efiscal.backend.model.ActionCatalogEntity;
import com.efiscal.backend.model.AppUserEntity;
import com.efiscal.backend.model.ClientEntity;
import com.efiscal.backend.model.RoleActionAccessEntity;
import com.efiscal.backend.model.RoleActionAccessId;
import com.efiscal.backend.model.RoleEntity;
import com.efiscal.backend.repository.ActionCatalogRepository;
import com.efiscal.backend.repository.AppUserRepository;
import com.efiscal.backend.repository.ClientRepository;
import com.efiscal.backend.repository.RoleActionAccessRepository;
import com.efiscal.backend.repository.RoleRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
    private final ActionCatalogRepository actionCatalogRepository;
    private final RoleActionAccessRepository roleActionAccessRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataInitializerService(
        RoleRepository roleRepository,
        ClientRepository clientRepository,
        AppUserRepository appUserRepository,
        ActionCatalogRepository actionCatalogRepository,
        RoleActionAccessRepository roleActionAccessRepository
    ) {
        this.roleRepository = roleRepository;
        this.clientRepository = clientRepository;
        this.appUserRepository = appUserRepository;
        this.actionCatalogRepository = actionCatalogRepository;
        this.roleActionAccessRepository = roleActionAccessRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Seed actions first
        seedActions();

        RoleEntity superAdminRole = seedRole(RoleEntity.ROLE_SUPERADMIN, "Super Administrator",
            "Full unrestricted access across all clients and organizations");

        RoleEntity clientAdminRole = seedRole("CLIENT_ADMIN", "Client Administrator",
            "Administrative access within an assigned client scope");

        RoleEntity operatorRole = seedRole("OPERATOR", "Operator",
            "Standard operational access for day-to-day tasks");

        // Seed role-action mappings
        seedRoleActions(superAdminRole, List.of(
            "FISCAL_CREATE_BILL", "FISCAL_RETRY_BILL", "FISCAL_VIEW_BILLS",
            "MERCHANTPRO_FETCH_ORDERS", "USERS_MANAGE", "ROLES_MANAGE", "ORGS_MANAGE"
        ));
        seedRoleActions(clientAdminRole, List.of(
            "FISCAL_CREATE_BILL", "FISCAL_RETRY_BILL", "FISCAL_VIEW_BILLS",
            "MERCHANTPRO_FETCH_ORDERS", "USERS_MANAGE", "ROLES_MANAGE", "ORGS_MANAGE"
        ));
        seedRoleActions(operatorRole, List.of(
            "FISCAL_VIEW_BILLS", "MERCHANTPRO_FETCH_ORDERS"
        ));

        ClientEntity globalClient = seedClient("Global", "ACTIVE", "RSD");
        ClientEntity acmeClient = seedClient("Acme Retail", "ACTIVE", "RSD");

        seedAdminUser(globalClient, superAdminRole);
        seedOpsUser(acmeClient, clientAdminRole);
    }

    private RoleEntity seedRole(String roleCode, String name, String description) {
        return roleRepository.findByRoleCodeAndClientIsNull(roleCode).orElseGet(() -> {
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

    private void seedOpsUser(ClientEntity client, RoleEntity role) {
        if (!appUserRepository.existsByEmail("ops@acme.rs")) {
            AppUserEntity ops = new AppUserEntity();
            ops.setEmail("ops@acme.rs");
            ops.setPasswordHash(passwordEncoder.encode("Ops123!"));
            ops.setFullName("Acme Operations");
            ops.setClient(client);
            ops.setRole(role);
            ops.setSubscriptionStatus("ACTIVE");
            ops.setSubscriptionStartAt(OffsetDateTime.now(ZoneOffset.UTC));
            ops.setSubscriptionExpiresAt(
                LocalDate.now().plusMonths(6).atStartOfDay().atOffset(ZoneOffset.UTC));
            ops.setActive(true);
            appUserRepository.save(ops);
            log.info("Seeded ops user: ops@acme.rs");
        }
    }

    private void seedActions() {
        seedAction("FISCAL", "FISCAL_CREATE_BILL", "Create Fiscal Bill", "Allows issuing new fiscal bills");
        seedAction("FISCAL", "FISCAL_RETRY_BILL", "Retry Fiscal Bill", "Allows retrying failed fiscal bills");
        seedAction("FISCAL", "FISCAL_VIEW_BILLS", "View Fiscal Bills", "Allows viewing fiscal bills list and details");
        seedAction("MERCHANTPRO", "MERCHANTPRO_FETCH_ORDERS", "Fetch Orders", "Allows fetching orders from MerchantPro API");
        seedAction("SYSTEM", "USERS_MANAGE", "Manage Users", "Allows creating and editing users");
        seedAction("SYSTEM", "ROLES_MANAGE", "Manage Roles", "Allows creating and editing roles");
        seedAction("SYSTEM", "ORGS_MANAGE", "Manage Organizations", "Allows managing organizations and API settings");
    }

    private void seedAction(String moduleCode, String actionCode, String name, String description) {
        if (actionCatalogRepository.findByActionCode(actionCode).isEmpty()) {
            ActionCatalogEntity action = new ActionCatalogEntity();
            action.setModuleCode(moduleCode);
            action.setActionCode(actionCode);
            action.setName(name);
            action.setDescription(description);
            action.setActive(true);
            actionCatalogRepository.save(action);
            log.info("Seeded action: {}", actionCode);
        }
    }

    private void seedRoleActions(RoleEntity role, java.util.List<String> actionCodes) {
        for (String code : actionCodes) {
            actionCatalogRepository.findByActionCode(code).ifPresent(action -> {
                RoleActionAccessId id = new RoleActionAccessId(role.getRoleId(), action.getActionId());
                if (roleActionAccessRepository.findById(id).isEmpty()) {
                    RoleActionAccessEntity access = new RoleActionAccessEntity();
                    access.setRoleId(role.getRoleId());
                    access.setActionId(action.getActionId());
                    access.setAllowed(true);
                    roleActionAccessRepository.save(access);
                    log.info("Seeded role action access: {} -> {}", role.getRoleCode(), code);
                }
            });
        }
    }
}

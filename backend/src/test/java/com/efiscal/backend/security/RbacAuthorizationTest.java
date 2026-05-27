package com.efiscal.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.efiscal.backend.service.UserManagementService;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RbacAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserManagementService userManagementService;

    private String login(String email, String password) throws Exception {
        String requestJson = objectMapper.writeValueAsString(Map.of(
            "email", email,
            "password", password
        ));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(responseBody, Map.class);
        return (String) responseMap.get("accessToken");
    }

    @Test
    public void testClientOrgsUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/clients-orgs"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testClientOrgsSuperAdminGetsAll() throws Exception {
        String token = login("admin@efiscal.local", "Admin123!");

        mockMvc.perform(get("/api/v1/clients-orgs")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].clientName", is("Acme Retail")))
                .andExpect(jsonPath("$[2].clientName", is("Beta Foods")));
    }

    @Test
    public void testClientOrgsTenantAdminGetsFiltered() throws Exception {
        String token = login("ops@acme.rs", "Ops123!");

        mockMvc.perform(get("/api/v1/clients-orgs")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].clientName", is("Acme Retail")))
                .andExpect(jsonPath("$[1].clientName", is("Acme Retail")))
                .andExpect(jsonPath("$[*].clientName", not(hasItem("Beta Foods"))));
    }

    @Test
    public void testSuperAdminCannotDeactivateBuiltInRoles() throws Exception {
        String token = login("admin@efiscal.local", "Admin123!");

        // First, let's find the SUPERADMIN role ID by listing roles
        MvcResult result = mockMvc.perform(get("/api/v1/roles")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        java.util.List<?> rolesList = objectMapper.readValue(responseBody, java.util.List.class);
        
        Long superAdminRoleId = null;
        for (Object rObj : rolesList) {
            Map<?, ?> roleMap = (Map<?, ?>) rObj;
            if ("SUPERADMIN".equals(roleMap.get("roleCode"))) {
                superAdminRoleId = ((Number) roleMap.get("roleId")).longValue();
                break;
            }
        }

        assert superAdminRoleId != null;

        // Try to update SUPERADMIN and set isActive to false
        String updateJson = objectMapper.writeValueAsString(Map.of(
            "isActive", false
        ));

        mockMvc.perform(put("/api/v1/roles/" + superAdminRoleId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Built-in system roles cannot be deactivated")));
    }

    @Test
    public void testTenantAdminCannotAssignSuperadminRole() throws Exception {
        String token = login("ops@acme.rs", "Ops123!");

        // Get SUPERADMIN role ID
        MvcResult rolesResult = mockMvc.perform(get("/api/v1/roles")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String rolesBody = rolesResult.getResponse().getContentAsString();
        java.util.List<?> rolesList = objectMapper.readValue(rolesBody, java.util.List.class);
        Long superAdminRoleId = null;
        for (Object rObj : rolesList) {
            Map<?, ?> roleMap = (Map<?, ?>) rObj;
            if ("SUPERADMIN".equals(roleMap.get("roleCode"))) {
                superAdminRoleId = ((Number) roleMap.get("roleId")).longValue();
                break;
            }
        }
        assert superAdminRoleId != null;

        // Get client ID of Acme Retail by listing users
        MvcResult usersResult = mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String usersBody = usersResult.getResponse().getContentAsString();
        java.util.List<?> usersList = objectMapper.readValue(usersBody, java.util.List.class);
        Long acmeClientId = null;
        for (Object uObj : usersList) {
            Map<?, ?> userMap = (Map<?, ?>) uObj;
            if ("ops@acme.rs".equals(userMap.get("email"))) {
                acmeClientId = ((Number) userMap.get("clientId")).longValue();
                break;
            }
        }
        assert acmeClientId != null;

        // Try to create a user for Acme Retail with SUPERADMIN role
        String createUserJson = objectMapper.writeValueAsString(Map.of(
            "email", "hacker@acme.rs",
            "password", "Hacker123!",
            "fullName", "Hacker User",
            "clientId", acmeClientId,
            "roleId", superAdminRoleId
        ));

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserJson))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testTenantAdminCannotReadOtherClientUsers() throws Exception {
        String token = login("ops@acme.rs", "Ops123!");

        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].clientName", everyItem(is("Acme Retail"))));
    }

    @Test
    public void testUnauthenticatedCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testTenantAdminCannotCreateUserForAnotherClient() throws Exception {
        // Get Global client ID by listing users as superadmin
        String adminToken = login("admin@efiscal.local", "Admin123!");
        MvcResult adminUsersResult = mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        String adminUsersBody = adminUsersResult.getResponse().getContentAsString();
        java.util.List<?> adminUsersList = objectMapper.readValue(adminUsersBody, java.util.List.class);
        Long globalClientId = null;
        for (Object uObj : adminUsersList) {
            Map<?, ?> userMap = (Map<?, ?>) uObj;
            if ("admin@efiscal.local".equals(userMap.get("email"))) {
                globalClientId = ((Number) userMap.get("clientId")).longValue();
                break;
            }
        }
        assert globalClientId != null;

        // Login as tenant admin
        String opsToken = login("ops@acme.rs", "Ops123!");

        // Get OPERATOR role ID
        MvcResult rolesResult = mockMvc.perform(get("/api/v1/roles")
                .header("Authorization", "Bearer " + opsToken))
                .andExpect(status().isOk())
                .andReturn();
        String rolesBody = rolesResult.getResponse().getContentAsString();
        java.util.List<?> rolesList = objectMapper.readValue(rolesBody, java.util.List.class);
        Long operatorRoleId = null;
        for (Object rObj : rolesList) {
            Map<?, ?> roleMap = (Map<?, ?>) rObj;
            if ("OPERATOR".equals(roleMap.get("roleCode"))) {
                operatorRoleId = ((Number) roleMap.get("roleId")).longValue();
                break;
            }
        }
        assert operatorRoleId != null;

        // Try to create a user under the Global client ID
        String createUserJson = objectMapper.writeValueAsString(Map.of(
            "email", "crossclient@efiscal.local",
            "password", "Cross123!",
            "fullName", "Cross Client User",
            "clientId", globalClientId,
            "roleId", operatorRoleId
        ));

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + opsToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserJson))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testListUsersThrowsWhenNotSuperadminAndClientIdNull() {
        Assertions.assertThrows(
            ResponseStatusException.class,
            () -> userManagementService.listUsers(null, false)
        );
    }
}

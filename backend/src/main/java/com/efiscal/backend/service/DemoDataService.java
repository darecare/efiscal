package com.efiscal.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DemoDataService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, UserAccount> usersByEmail = new ConcurrentHashMap<>();
    private final Map<String, AuthenticatedUser> sessionsByToken = new ConcurrentHashMap<>();
    private final List<ClientOrgView> clientOrgs;
    private final List<ApiConnectionView> apiConnections;
    private final List<ApiTemplateView> apiTemplates;
    private final List<OrderView> orders;

    public DemoDataService() {
        UserAccount superAdmin = new UserAccount(
            UUID.randomUUID().toString(),
            "admin@efiscal.local",
            passwordEncoder.encode("Admin123!"),
            "System Superadmin",
            "SUPERADMIN",
            "Global",
            "ACTIVE",
            null);
        UserAccount manager = new UserAccount(
            UUID.randomUUID().toString(),
            "ops@acme.rs",
            passwordEncoder.encode("Ops123!"),
            "Acme Operations",
            "CLIENT_ADMIN",
            "Acme Retail",
            "ACTIVE",
            LocalDate.now().plusMonths(6).toString());
        usersByEmail.put(superAdmin.email(), superAdmin);
        usersByEmail.put(manager.email(), manager);

        clientOrgs = List.of(
            new ClientOrgView("client-acme", "Acme Retail", "org-acme-hq", "Acme HQ", "ACTIVE", "RSD"),
            new ClientOrgView("client-acme", "Acme Retail", "org-acme-web", "Acme Webshop", "ACTIVE", "RSD"),
            new ClientOrgView("client-beta", "Beta Foods", "org-beta-main", "Beta Main", "SETUP", "EUR"));

        apiConnections = List.of(
            new ApiConnectionView("conn-mp", "MerchantPro Serbia", "MERCHANTPRO", "CONFIG_ONLY", "https://api.merchantpro.rs"),
            new ApiConnectionView("conn-tax", "Test Tax Authority", "TAX_CORE", "PLUGIN", "https://test.suf.purs.gov.rs"));

        apiTemplates = List.of(
            new ApiTemplateView("tmpl-orders", "MerchantPro Orders Fetch", "FETCH_ORDERS", "GET", List.of("startDate", "endDate", "shippingStatus")),
            new ApiTemplateView("tmpl-auth", "MerchantPro Auth", "AUTH_LOGIN", "POST", List.of("username", "apiKey")));

        orders = List.of(
            new OrderView("1", "MP-100045", "Milica Jovanovic", "ready_to_ship", new BigDecimal("14990.00"), "2026-04-24T10:30:00"),
            new OrderView("2", "MP-100046", "Petar Markovic", "processing", new BigDecimal("8990.00"), "2026-04-24T11:10:00"),
            new OrderView("3", "MP-100047", "Ana Ilic", "completed", new BigDecimal("23990.00"), "2026-04-23T15:45:00"));
    }

    public LoginResult login(String email, String password) {
        UserAccount account = usersByEmail.get(email);
        if (account == null || !passwordEncoder.matches(password, account.passwordHash())) {
            return null;
        }
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
            account.id(),
            account.email(),
            account.fullName(),
            account.roleName(),
            account.clientName(),
            account.subscriptionStatus(),
            account.subscriptionExpiresAt());
        String token = UUID.randomUUID().toString();
        sessionsByToken.put(token, authenticatedUser);
        return new LoginResult(token, authenticatedUser);
    }

    public AuthenticatedUser findByToken(String token) {
        return sessionsByToken.get(token);
    }

    public List<AuthenticatedUser> listUsers() {
        return usersByEmail.values().stream()
            .map(account -> new AuthenticatedUser(
                account.id(),
                account.email(),
                account.fullName(),
                account.roleName(),
                account.clientName(),
                account.subscriptionStatus(),
                account.subscriptionExpiresAt()))
            .toList();
    }

    public List<ClientOrgView> listClientOrgs() {
        return clientOrgs;
    }

    public List<ApiConnectionView> listApiConnections() {
        return apiConnections;
    }

    public List<ApiTemplateView> listApiTemplates() {
        return apiTemplates;
    }

    public List<OrderView> findOrders(OrderFilter filter) {
        return orders.stream()
            .filter(order -> filter.shippingStatus() == null || filter.shippingStatus().isBlank() || order.shippingStatus().equalsIgnoreCase(filter.shippingStatus()))
            .toList();
    }

    public record LoginResult(String accessToken, AuthenticatedUser user) {
    }

    private record UserAccount(
        String id,
        String email,
        String passwordHash,
        String fullName,
        String roleName,
        String clientName,
        String subscriptionStatus,
        String subscriptionExpiresAt) {
    }

    public record AuthenticatedUser(
        String id,
        String email,
        String fullName,
        String roleName,
        String clientName,
        String subscriptionStatus,
        String subscriptionExpiresAt) {

        public List<SimpleGrantedAuthority> authorities() {
            return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
        }
    }

    public record ClientOrgView(
        String clientId,
        String clientName,
        String orgId,
        String orgName,
        String status,
        String currency) {
    }

    public record ApiConnectionView(
        String id,
        String name,
        String provider,
        String mode,
        String baseUrl) {
    }

    public record ApiTemplateView(
        String id,
        String name,
        String operation,
        String method,
        List<String> parameters) {
    }

    public record OrderView(
        String id,
        String externalOrderNo,
        String customerName,
        String shippingStatus,
        BigDecimal totalAmount,
        String createdAt) {
    }

    public record OrderFilter(
        String startDate,
        String endDate,
        String shippingStatus) {
    }
}

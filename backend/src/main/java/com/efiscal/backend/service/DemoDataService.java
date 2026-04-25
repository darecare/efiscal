package com.efiscal.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    private final Map<String, FiscalBillView> fiscalBillsById = new ConcurrentHashMap<>();
    private final Map<String, String> fiscalBillIdByIdempotencyKey = new ConcurrentHashMap<>();
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
            new OrderView("1", "MP-100045", "Milica Jovanovic", "ready_to_ship", new BigDecimal("14990.00"), "2026-04-24T10:30:00",
                List.of(new OrderLineView("P1", "Laptop Stand Aluminium", "SKU-LS-01", "1", "14990.00"))),
            new OrderView("2", "MP-100046", "Petar Markovic", "processing", new BigDecimal("8990.00"), "2026-04-24T11:10:00",
                List.of(new OrderLineView("P2", "Wireless Mouse", "SKU-WM-02", "1", "4990.00"),
                        new OrderLineView("P3", "USB-C Hub 7-in-1", "SKU-HUB-03", "1", "4000.00"))),
            new OrderView("3", "MP-100047", "Ana Ilic", "completed", new BigDecimal("23990.00"), "2026-04-23T15:45:00",
                List.of(new OrderLineView("P4", "Mechanical Keyboard TKL", "SKU-KB-04", "1", "15990.00"),
                        new OrderLineView("P5", "Mouse Pad XL", "SKU-MP-05", "2", "4000.00"))));
    }

    public LoginResult login(String email, String password) {
        UserAccount account = usersByEmail.get(email);
        if (account == null || !passwordEncoder.matches(password, account.passwordHash())) {
            return null;
        }
        if (!isAccessAllowed(account.roleName(), account.subscriptionStatus(), account.subscriptionExpiresAt())) {
            return LoginResult.subscriptionExpired();
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
        AuthenticatedUser user = sessionsByToken.get(token);
        if (user == null) {
            return null;
        }
        if (isAccessAllowed(user.roleName(), user.subscriptionStatus(), user.subscriptionExpiresAt())) {
            return user;
        }
        sessionsByToken.remove(token);
        return null;
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

    public FiscalBillCreateResult createFiscalBill(String idempotencyKey, FiscalBillCreateRequest request) {
        String existingId = fiscalBillIdByIdempotencyKey.get(idempotencyKey);
        if (existingId != null) {
            return FiscalBillCreateResult.ofAlreadyExists(fiscalBillsById.get(existingId));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        FiscalBillStatus nextStatus = resolveInitialFiscalStatus(request.orderId());
        String documentId = UUID.randomUUID().toString();
        String providerReference = nextStatus == FiscalBillStatus.SUCCESS ? "SUF-" + request.orderId() + "-" + now.toEpochSecond() : null;
        String lastError = nextStatus == FiscalBillStatus.FAILED ? "Provider timeout. Retry is allowed." : null;

        FiscalBillView view = new FiscalBillView(
            documentId,
            request.orderId(),
            nextStatus.name(),
            providerReference,
            lastError,
            1,
            now.toString(),
            now.toString());

        fiscalBillsById.put(documentId, view);
        fiscalBillIdByIdempotencyKey.put(idempotencyKey, documentId);
        return FiscalBillCreateResult.ofCreated(view);
    }

    public FiscalBillView findFiscalBillById(String fiscalBillId) {
        return fiscalBillsById.get(fiscalBillId);
    }

    public FiscalBillRetryResult retryFiscalBill(String fiscalBillId, String idempotencyKey) {
        String existingRetryId = fiscalBillIdByIdempotencyKey.get(idempotencyKey);
        if (existingRetryId != null && !existingRetryId.equals(fiscalBillId)) {
            return FiscalBillRetryResult.ofIdempotencyConflict();
        }

        FiscalBillView current = fiscalBillsById.get(fiscalBillId);
        if (current == null) {
            return FiscalBillRetryResult.ofNotFound();
        }
        if (!"FAILED".equals(current.status())) {
            return FiscalBillRetryResult.ofNotRetryable(current);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        FiscalBillView retried = new FiscalBillView(
            current.fiscalDocumentId(),
            current.orderId(),
            FiscalBillStatus.RETRYING.name(),
            current.providerReference(),
            null,
            current.attemptCount() + 1,
            current.createdAt(),
            now.toString());
        fiscalBillsById.put(fiscalBillId, retried);
        fiscalBillIdByIdempotencyKey.put(idempotencyKey, fiscalBillId);
        return FiscalBillRetryResult.ofRetried(retried);
    }

    private FiscalBillStatus resolveInitialFiscalStatus(String orderId) {
        return "2".equals(orderId) ? FiscalBillStatus.FAILED : FiscalBillStatus.SUCCESS;
    }

    private boolean isAccessAllowed(String roleName, String subscriptionStatus, String subscriptionExpiresAt) {
        if ("SUPERADMIN".equals(roleName)) {
            return true;
        }
        if (!"ACTIVE".equalsIgnoreCase(subscriptionStatus)) {
            return false;
        }
        if (subscriptionExpiresAt == null || subscriptionExpiresAt.isBlank()) {
            return false;
        }
        return !LocalDate.parse(subscriptionExpiresAt).isBefore(LocalDate.now());
    }

    public record LoginResult(String accessToken, AuthenticatedUser user) {
        public static LoginResult subscriptionExpired() {
            return new LoginResult(null, null);
        }

        public boolean isSubscriptionExpired() {
            return accessToken == null && user == null;
        }
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

    public record OrderLineView(
        String productId,
        String productName,
        String sku,
        String quantity,
        String unitPrice) {
    }

    public record OrderView(
        String id,
        String externalOrderNo,
        String customerName,
        String shippingStatus,
        BigDecimal totalAmount,
        String createdAt,
        List<OrderLineView> orderLines) {
    }

    public record OrderFilter(
        String startDate,
        String endDate,
        String shippingStatus) {
    }

    public record FiscalBillCreateRequest(
        String orderId,
        CustomerPayload customer,
        List<FiscalItemPayload> items,
        String currency,
        String paymentMethod) {
    }

    public record CustomerPayload(String name) {
    }

    public record FiscalItemPayload(
        String sku,
        String name,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate) {
    }

    public record FiscalBillCreateResult(FiscalBillView fiscalBill, boolean created, boolean idempotencyConflict) {
        public static FiscalBillCreateResult ofCreated(FiscalBillView fiscalBill) {
            return new FiscalBillCreateResult(fiscalBill, true, false);
        }

        public static FiscalBillCreateResult ofAlreadyExists(FiscalBillView fiscalBill) {
            return new FiscalBillCreateResult(fiscalBill, false, false);
        }
    }

    public record FiscalBillRetryResult(FiscalBillView fiscalBill, boolean retried, boolean notFound, boolean notRetryable, boolean idempotencyConflict) {
        public static FiscalBillRetryResult ofRetried(FiscalBillView fiscalBill) {
            return new FiscalBillRetryResult(fiscalBill, true, false, false, false);
        }

        public static FiscalBillRetryResult ofNotFound() {
            return new FiscalBillRetryResult(null, false, true, false, false);
        }

        public static FiscalBillRetryResult ofNotRetryable(FiscalBillView fiscalBill) {
            return new FiscalBillRetryResult(fiscalBill, false, false, true, false);
        }

        public static FiscalBillRetryResult ofIdempotencyConflict() {
            return new FiscalBillRetryResult(null, false, false, false, true);
        }
    }

    public record FiscalBillView(
        String fiscalDocumentId,
        String orderId,
        String status,
        String providerReference,
        String lastError,
        int attemptCount,
        String createdAt,
        String updatedAt) {
    }

    public enum FiscalBillStatus {
        SUCCESS,
        FAILED,
        RETRYING
    }
}

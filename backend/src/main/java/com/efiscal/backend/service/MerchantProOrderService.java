package com.efiscal.backend.service;

import com.efiscal.backend.model.ApiConnEntity;
import com.efiscal.backend.model.ApiTemplateEntity;
import com.efiscal.backend.repository.ApiConnRepository;
import com.efiscal.backend.repository.ApiTemplateRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.URISyntaxException;

@Service
public class MerchantProOrderService {

    private final ApiConnRepository apiConnRepository;
    private final ApiTemplateRepository apiTemplateRepository;
    private final RestTemplate restTemplate;

    public MerchantProOrderService(ApiConnRepository apiConnRepository,
                                   ApiTemplateRepository apiTemplateRepository,
                                   RestTemplate restTemplate) {
        this.apiConnRepository = apiConnRepository;
        this.apiTemplateRepository = apiTemplateRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional(readOnly = true)
    public OrderFetchResult fetchOrders(Long orgId, String createdAfter, String shippingStatus, int start, int limit) {
        // 1. Resolve active MERCHANTPRO apiconn for this org
        ApiConnEntity conn = apiConnRepository
            .findAllByOrgOrgIdAndDeletedAtIsNull(orgId)
            .stream()
            .filter(c -> "MP".equals(c.getApiPlatform()) && c.isActive())
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No active MerchantPro connection configured for this organization"));

        // 2. Resolve FETCH_ORDERS apitemplate
        ApiTemplateEntity template = apiTemplateRepository
            .findAllByApiConnApiconnId(conn.getApiconnId())
            .stream()
            .filter(t -> "FETCH_ORDERS".equals(t.getOperationKey()) && t.isActive())
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No active FETCH_ORDERS template found for this connection"));

        // 3. Build URL with query params
        // Use pre-encoded %5B%5D for created[gt] because UriComponentsBuilder
        // double-encodes brackets when passed through RestTemplate's String URL handler
        String apiBase = conn.getApiBaseUrl() != null ? conn.getApiBaseUrl() : "";
        if (!apiBase.endsWith("/")) apiBase += "/";
        String rawUrl = apiBase + template.getEndpointPath()
            + "?limit=" + limit + "&sort=date_created.desc&start=" + start + "&include=line_items";
        if (createdAfter != null && !createdAfter.isBlank()) {
            rawUrl += "&created%5Bgt%5D=" + createdAfter;
        }
        if (shippingStatus != null && !shippingStatus.isBlank()) {
            rawUrl += "&shipping_status=" + shippingStatus;
        }
        URI uri;
        try {
            uri = new URI(rawUrl);
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid API URL: " + e.getMessage());
        }

        // 4. Build auth headers
        HttpHeaders headers = new HttpHeaders();
        if ("BASIC_AUTH".equals(conn.getApiauthtype())
                && conn.getApikey() != null && !conn.getApikey().isBlank()
                && conn.getApisecret() != null && !conn.getApisecret().isBlank()) {
            String creds = conn.getApikey() + ":" + conn.getApisecret();
            String encoded = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encoded);
        }
        headers.set("Accept", "application/json");

        // 5. Execute HTTP call
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<?> response;
        try {
            response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "MerchantPro API call failed: " + ex.getMessage());
        }

        // 6. Parse response { data: [...], meta: { count: { total: N, ... }, links: {...} } }
        Object bodyObj = response.getBody();
        if (!(bodyObj instanceof Map<?, ?> body)) {
            return new OrderFetchResult(Collections.emptyList(), 0);
        }
        List<Map<String, Object>> rawOrders = toMapList(body.get("data"));
        int total = rawOrders.size();
        if (body.containsKey("meta")) {
            Map<String, Object> meta = toStringObjectMap(body.get("meta"));
            // MerchantPro nests total under meta.count.total
            Object countObj = meta.get("count");
            if (countObj instanceof Map<?, ?> count) {
                Object t = count.get("total");
                if (t instanceof Number n) total = n.intValue();
            } else {
                // fallback for flat meta.total
                Object t = meta.get("total");
                if (t instanceof Number n) total = n.intValue();
            }
        }

        List<DemoDataService.OrderView> orders = rawOrders.stream().map(this::mapOrder).toList();
        return new OrderFetchResult(orders, total);
    }

    private DemoDataService.OrderView mapOrder(Map<String, Object> raw) {
        String id = str(raw.getOrDefault("id", raw.getOrDefault("order_id", "")));
        String orderNo = str(raw.getOrDefault("order_number", raw.getOrDefault("reference", id)));
        String shippingStatus = str(raw.getOrDefault("shipping_status", ""));
        Object totalAmt = raw.getOrDefault("total_amount", raw.getOrDefault("total", "0"));
        BigDecimal total;
        try {
            total = new BigDecimal(str(totalAmt).replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            total = BigDecimal.ZERO;
        }
        String createdAt = str(raw.getOrDefault("date_created", raw.getOrDefault("created_at", "")));

        // Extract customer name: MerchantPro uses billing_name at top level
        String customer = str(raw.getOrDefault("billing_name", ""));
        if (customer.isEmpty()) {
            customer = str(raw.getOrDefault("customer_name", raw.getOrDefault("customer", "")));
        }
        String customerEmail = extractCustomerEmail(raw);
        String paymentMethodCode = str(raw.getOrDefault("payment_method_code", raw.getOrDefault("paymentMethodCode", "")));

        // Extract order lines — MerchantPro returns them under 'line_items' when include=line_items
        List<DemoDataService.OrderLineView> orderLines = new java.util.ArrayList<>();
        Object linesObj = raw.getOrDefault("line_items", raw.getOrDefault("order_lines", raw.getOrDefault("products", raw.get("items"))));
        if (linesObj instanceof List<?> rawLines) {
            for (Object lineObj : rawLines) {
                if (lineObj instanceof Map<?, ?> line) {
                    Map<String, Object> l = toStringObjectMap(line);
                    String productId   = str(l.getOrDefault("product_id", l.getOrDefault("id", "")));
                    String productName = str(l.getOrDefault("product_name", l.getOrDefault("name", "")));
                    String sku         = str(l.getOrDefault("product_sku", l.getOrDefault("sku", "")));
                    String qty         = str(l.getOrDefault("quantity", l.getOrDefault("qty", "")));
                    String unitPrice   = str(l.getOrDefault("unit_price_gross", l.getOrDefault("unit_price_net", l.getOrDefault("price", ""))));
                    String taxValue    = str(l.getOrDefault("product_tax_percent",
                            l.getOrDefault("tax_value", l.getOrDefault("tax_rate", ""))));
                    String taxCategoryName = str(l.getOrDefault("product_tax_name", l.getOrDefault("tax_name", "")));
                    orderLines.add(new DemoDataService.OrderLineView(productId, productName, sku, qty, unitPrice, taxValue, taxCategoryName));
                }
            }
        }

        return new DemoDataService.OrderView(id, orderNo, customer, customerEmail, shippingStatus, total, createdAt, paymentMethodCode, orderLines);
    }

    private String extractCustomerEmail(Map<String, Object> raw) {
        String email = str(raw.getOrDefault("billing_email", ""));
        if (!email.isBlank()) return email;

        email = str(raw.getOrDefault("customer_email", raw.getOrDefault("email", "")));
        if (!email.isBlank()) return email;

        return str(raw.getOrDefault("billing_email_address", raw.getOrDefault("customerEmail", "")));
    }

    private List<Map<String, Object>> toMapList(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Object element : rawList) {
            Map<String, Object> map = toStringObjectMap(element);
            if (!map.isEmpty()) {
                result.add(map);
            }
        }
        return result;
    }

    private Map<String, Object> toStringObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new java.util.HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    public record OrderFetchResult(List<DemoDataService.OrderView> data, int total) {}
}

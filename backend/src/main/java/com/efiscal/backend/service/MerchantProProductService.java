package com.efiscal.backend.service;

import com.efiscal.backend.model.ApiConnEntity;
import com.efiscal.backend.model.ApiTemplateEntity;
import com.efiscal.backend.repository.ApiConnRepository;
import com.efiscal.backend.repository.ApiTemplateRepository;
import java.net.URI;
import java.net.URISyntaxException;
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

@Service
public class MerchantProProductService {

    private static final String FIELDS = "id,name,sku,ean,price_gross";

    private final ApiConnRepository apiConnRepository;
    private final ApiTemplateRepository apiTemplateRepository;
    private final RestTemplate restTemplate;

    public MerchantProProductService(
        ApiConnRepository apiConnRepository,
        ApiTemplateRepository apiTemplateRepository,
        RestTemplate restTemplate
    ) {
        this.apiConnRepository = apiConnRepository;
        this.apiTemplateRepository = apiTemplateRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional(readOnly = true)
    public ProductFetchResult fetchProducts(Long orgId, String skuEquals, String eanEquals, int start, int limit) {
        ApiConnEntity conn = resolveConnection(orgId);
        ApiTemplateEntity template = resolveTemplate(conn);

        String apiBase = conn.getApiBaseUrl() != null ? conn.getApiBaseUrl() : "";
        if (!apiBase.endsWith("/")) {
            apiBase += "/";
        }

        StringBuilder url = new StringBuilder(apiBase)
            .append(template.getEndpointPath())
            .append("?fields=").append(FIELDS)
            .append("&start=").append(start)
            .append("&limit=").append(limit);

        if (skuEquals != null && !skuEquals.isBlank()) {
            url.append("&sku_equals=").append(encodeQueryValue(skuEquals.trim()));
        }
        if (eanEquals != null && !eanEquals.isBlank()) {
            url.append("&ean_equals=").append(encodeQueryValue(eanEquals.trim()));
        }

        URI uri = buildUri(url.toString());
        Map<?, ?> body = executeGet(conn, uri);
        return parseResponse(body);
    }

    private ApiConnEntity resolveConnection(Long orgId) {
        return apiConnRepository
            .findAllByOrgOrgIdAndDeletedAtIsNull(orgId)
            .stream()
            .filter(c -> "MP".equals(c.getApiPlatform()) && c.isActive())
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No active MerchantPro connection configured for this organization"));
    }

    private ApiTemplateEntity resolveTemplate(ApiConnEntity conn) {
        return apiTemplateRepository
            .findAllByApiConnApiconnId(conn.getApiconnId())
            .stream()
            .filter(t -> "GET_PRODUCTS".equals(t.getOperationKey()) && t.isActive())
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No active GET_PRODUCTS template found for this connection"));
    }

    private URI buildUri(String rawUrl) {
        try {
            return new URI(rawUrl);
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid API URL: " + e.getMessage());
        }
    }

    private Map<?, ?> executeGet(ApiConnEntity conn, URI uri) {
        HttpHeaders headers = new HttpHeaders();
        if ("BASIC_AUTH".equals(conn.getApiauthtype())
                && conn.getApikey() != null && !conn.getApikey().isBlank()
                && conn.getApisecret() != null && !conn.getApisecret().isBlank()) {
            String creds = conn.getApikey() + ":" + conn.getApisecret();
            String encoded = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encoded);
        }
        headers.set("Accept", "application/json");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
            return response.getBody() != null ? response.getBody() : Collections.emptyMap();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "MerchantPro products API call failed: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private ProductFetchResult parseResponse(Map<?, ?> body) {
        if (body == null || body.isEmpty()) {
            return new ProductFetchResult(Collections.emptyList(), 0);
        }
        List<Map<String, Object>> rawProducts = body.containsKey("data")
            ? (List<Map<String, Object>>) body.get("data")
            : Collections.emptyList();

        int total = rawProducts.size();
        if (body.containsKey("meta")) {
            Map<String, Object> meta = (Map<String, Object>) body.get("meta");
            Object countObj = meta.get("count");
            if (countObj instanceof Map<?, ?> count) {
                Object t = count.get("total");
                if (t instanceof Number n) {
                    total = n.intValue();
                }
            } else {
                Object t = meta.get("total");
                if (t instanceof Number n) {
                    total = n.intValue();
                }
            }
        }

        List<MerchantProProductRow> products = rawProducts.stream().map(this::mapProduct).toList();
        return new ProductFetchResult(products, total);
    }

    private MerchantProProductRow mapProduct(Map<String, Object> raw) {
        Integer id = toInteger(raw.get("id"));
        String name = str(raw.get("name"));
        String sku = str(raw.get("sku"));
        String ean = str(raw.get("ean"));
        java.math.BigDecimal priceGross = toBigDecimal(raw.get("price_gross"));
        return new MerchantProProductRow(id, name, sku, ean, priceGross);
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static Integer toInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static java.math.BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        try {
            return new java.math.BigDecimal(String.valueOf(o).replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String encodeQueryValue(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    public record MerchantProProductRow(
        Integer mpProductId,
        String name,
        String sku,
        String ean,
        java.math.BigDecimal priceGross
    ) {}

    public record ProductFetchResult(List<MerchantProProductRow> data, int total) {}
}

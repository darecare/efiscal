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
import java.util.Optional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MerchantProProductService {

    private static final String FIELDS = "id,name,sku,ean,price_gross";
    private static final int MAX_429_RETRIES = 3;
    private static final long[] RETRY_BACKOFF_MS = { 60_000L, 120_000L, 240_000L };

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
    public ProductFetchResult fetchProducts(Long orgId, int start, int limit) {
        ApiConnEntity conn = resolveConnection(orgId);
        ApiTemplateEntity template = resolveTemplate(conn);

        String apiBase = normalizeApiBase(conn.getApiBaseUrl());
        String url = apiBase
            + template.getEndpointPath()
            + "?fields=" + FIELDS
            + "&start=" + start
            + "&limit=" + limit;

        URI uri = buildUri(url);
        Map<?, ?> body = executeGet(conn, uri);
        return parseCollectionResponse(body);
    }

    @Transactional(readOnly = true)
    public Optional<MerchantProProductRow> fetchInventoryByIdentifier(Long orgId, String type, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        ApiConnEntity conn = resolveConnection(orgId);
        String apiBase = normalizeApiBase(conn.getApiBaseUrl());
        String encoded = encodePathSegment(identifier.trim());
        String url = apiBase + "api/v2/inventory/" + type + "/" + encoded + "?fields=" + FIELDS;

        URI uri = buildUri(url);
        Map<?, ?> body = executeGet(conn, uri);
        return parseIndividualResponse(body);
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

    private static String normalizeApiBase(String apiBaseUrl) {
        String apiBase = apiBaseUrl != null ? apiBaseUrl : "";
        if (!apiBase.endsWith("/")) {
            apiBase += "/";
        }
        return apiBase;
    }

    private URI buildUri(String rawUrl) {
        try {
            return new URI(rawUrl);
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid API URL: " + e.getMessage());
        }
    }

    private Map<?, ?> executeGet(ApiConnEntity conn, URI uri) {
        HttpHeaders headers = buildAuthHeaders(conn);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (int attempt = 0; attempt <= MAX_429_RETRIES; attempt++) {
            try {
                ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
                return response.getBody() != null ? response.getBody() : Collections.emptyMap();
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode().value() == 429) {
                    if (attempt >= MAX_429_RETRIES) {
                        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                            "MerchantPro API rate limit exceeded. Please try again later.");
                    }
                    sleepQuietly(RETRY_BACKOFF_MS[attempt]);
                    continue;
                }
                throw new ResponseStatusException(
                    HttpStatus.valueOf(ex.getStatusCode().value()),
                    decodeErrorMessage(ex.getResponseBodyAsString(), ex.getMessage())
                );
            } catch (ResponseStatusException rex) {
                throw rex;
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "MerchantPro products API call failed: " + ex.getMessage());
            }
        }
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
            "MerchantPro API rate limit exceeded. Please try again later.");
    }

    private static HttpHeaders buildAuthHeaders(ApiConnEntity conn) {
        HttpHeaders headers = new HttpHeaders();
        if ("BASIC_AUTH".equals(conn.getApiauthtype())
                && conn.getApikey() != null && !conn.getApikey().isBlank()
                && conn.getApisecret() != null && !conn.getApisecret().isBlank()) {
            String creds = conn.getApikey() + ":" + conn.getApisecret();
            String encoded = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encoded);
        }
        headers.set("Accept", "application/json");
        return headers;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "MerchantPro API retry interrupted");
        }
    }

    @SuppressWarnings("unchecked")
    private ProductFetchResult parseCollectionResponse(Map<?, ?> body) {
        if (body == null || body.isEmpty()) {
            return new ProductFetchResult(Collections.emptyList(), 0, null);
        }
        List<Map<String, Object>> rawProducts = body.containsKey("data")
            ? (List<Map<String, Object>>) body.get("data")
            : Collections.emptyList();

        int total = rawProducts.size();
        String nextLink = null;
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
            if (meta.containsKey("links")) {
                Object linksObj = meta.get("links");
                if (linksObj instanceof Map<?, ?> links) {
                    Object next = links.get("next");
                    if (next != null && !"null".equals(String.valueOf(next)) && !String.valueOf(next).isBlank()) {
                        nextLink = String.valueOf(next);
                    }
                }
            }
        }

        List<MerchantProProductRow> products = rawProducts.stream().map(this::mapProduct).toList();
        return new ProductFetchResult(products, total, nextLink);
    }

    @SuppressWarnings("unchecked")
    private Optional<MerchantProProductRow> parseIndividualResponse(Map<?, ?> body) {
        if (body == null || body.isEmpty()) {
            return Optional.empty();
        }
        if (body.containsKey("data") && body.get("data") instanceof Map<?, ?> dataMap) {
            return Optional.of(mapProduct((Map<String, Object>) dataMap));
        }
        if (body.containsKey("id")) {
            return Optional.of(mapProduct((Map<String, Object>) body));
        }
        return Optional.empty();
    }

    private MerchantProProductRow mapProduct(Map<String, Object> raw) {
        Long id = toLong(raw.get("id"));
        String name = str(raw.get("name"));
        String sku = str(raw.get("sku"));
        String ean = str(raw.get("ean"));
        java.math.BigDecimal priceGross = toBigDecimal(raw.get("price_gross"));
        return new MerchantProProductRow(id, name, sku, ean, priceGross);
    }

    private static String decodeErrorMessage(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<?, ?> parsed = mapper.readValue(body, Map.class);
            if (parsed.containsKey("error")) {
                Object err = parsed.get("error");
                if (err instanceof Map<?, ?> errMap) {
                    Object msg = errMap.get("message");
                    if (msg != null) {
                        return String.valueOf(msg);
                    }
                }
                if (err instanceof String s) {
                    return s;
                }
            }
            if (parsed.containsKey("message")) {
                return String.valueOf(parsed.get("message"));
            }
        } catch (Exception ignored) {
            /* use fallback */
        }
        return fallback;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(o));
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

    private static String encodePathSegment(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    public record MerchantProProductRow(
        Long mpProductId,
        String name,
        String sku,
        String ean,
        java.math.BigDecimal priceGross
    ) {}

    public record ProductFetchResult(List<MerchantProProductRow> data, int total, String nextLink) {}
}

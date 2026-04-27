package com.efiscal.backend.service;

import com.efiscal.backend.model.ApiConnEntity;
import com.efiscal.backend.model.ApiTemplateEntity;
import com.efiscal.backend.repository.ApiConnRepository;
import com.efiscal.backend.repository.ApiTemplateRepository;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Generic HTTP connector for Serbian Tax Authority (V-SDC) API.
 * Uses client PKCS12 certificate authentication with PAC header as required by V-SDC.
 * Supports both GET (no body) and POST/other methods (with optional request body).
 */
@Service
public class TaxAuthorityService {

    private static final Logger log = LoggerFactory.getLogger(TaxAuthorityService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final ApiConnRepository apiConnRepository;
    private final ApiTemplateRepository apiTemplateRepository;

    public TaxAuthorityService(ApiConnRepository apiConnRepository,
                               ApiTemplateRepository apiTemplateRepository) {
        this.apiConnRepository = apiConnRepository;
        this.apiTemplateRepository = apiTemplateRepository;
    }

    /**
     * Resolves the active FS (Fiscal System) connection and the named operation template
     * for the given org, then executes the HTTP call to the Tax Authority.
     *
     * @param orgId        organization ID
     * @param operationKey template operation key (e.g. "GET_STATUS")
     * @param requestBody  JSON body string; pass null or empty for GET requests
     * @return raw JSON response body as String
     */
    public String call(Long orgId, String operationKey, String requestBody) {
        ApiConnEntity conn = apiConnRepository
            .findAllByOrgOrgIdAndDeletedAtIsNull(orgId)
            .stream()
            .filter(c -> "FS".equals(c.getApiPlatform()) && c.isActive())
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No active Fiscal System (FS) API connection configured for this organization"));

        ApiTemplateEntity template = apiTemplateRepository
            .findAllByApiConnApiconnId(conn.getApiconnId())
            .stream()
            .filter(t -> operationKey.equals(t.getOperationKey()) && t.isActive())
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No active template found for operation: " + operationKey));

        return executeRequest(conn, template, requestBody);
    }

    private String executeRequest(ApiConnEntity conn, ApiTemplateEntity template, String requestBody) {
        String baseUrl = conn.getApiBaseUrl() != null ? conn.getApiBaseUrl() : "";
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        String endpointPath = template.getEndpointPath() != null ? template.getEndpointPath() : "";
        if (endpointPath.startsWith("/")) endpointPath = endpointPath.substring(1);
        String fullUrl = baseUrl + endpointPath;

        HttpClient client = buildHttpClient(conn);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(fullUrl))
            .timeout(TIMEOUT)
            .version(HttpClient.Version.HTTP_1_1)
            .header("Accept", "application/json");

        // PAC header is required for V-SDC
        if (conn.getPac() != null && !conn.getPac().isBlank()) {
            reqBuilder.header("PAC", conn.getPac());
        }

        String method = template.getHttpMethod() != null ? template.getHttpMethod().toUpperCase() : "GET";
        String contentType = template.getContentType() != null ? template.getContentType() : "application/json";

        if ("GET".equals(method)) {
            reqBuilder.GET();
        } else {
            String body = requestBody != null ? requestBody : "";
            reqBuilder.header("Content-Type", contentType);
            reqBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
        }

        HttpRequest request = reqBuilder.build();

        log.info("Tax Authority API call: {} {}", method, fullUrl);

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            log.info("Tax Authority API response status: {}", status);
            if (status == 200) {
                return response.body();
            }
            log.warn("Tax Authority API non-200 response: {} body={}", status, response.body());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Tax Authority API returned HTTP " + status + ": " + response.body());
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ex) {
            log.error("Tax Authority API call failed", ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Tax Authority API call failed: " + ex.getMessage());
        }
    }

    /**
     * Builds an HttpClient configured with the PKCS12 client certificate from cert_data.
     * Falls back to a plain HttpClient if no certificate is configured.
     */
    private HttpClient buildHttpClient(ApiConnEntity conn) {
        byte[] certData = conn.getCertData();
        String certPassword = conn.getCertPassword();

        if (certData != null && certData.length > 0) {
            try {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                char[] passwordChars = certPassword != null ? certPassword.toCharArray() : new char[0];
                keyStore.load(new ByteArrayInputStream(certData), passwordChars);

                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, passwordChars);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

                return HttpClient.newBuilder()
                    .version(Version.HTTP_1_1)
                    .sslContext(sslContext)
                    .connectTimeout(TIMEOUT)
                    .build();
            } catch (Exception ex) {
                log.error("Failed to initialise SSL context from certificate, falling back to default TLS", ex);
            }
        }

        return HttpClient.newBuilder()
            .version(Version.HTTP_1_1)
            .connectTimeout(TIMEOUT)
            .build();
    }
}

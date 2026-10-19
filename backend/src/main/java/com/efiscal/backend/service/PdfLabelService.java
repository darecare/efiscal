package com.efiscal.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * PDF receipt labels from classpath {@code locales/sr.json} ({@code pdf.latin} / {@code pdf.cyrillic}).
 * A4 always uses Serbian Latin; roll80 always uses Serbian Cyrillic.
 * Keep in sync with {@code frontend/src/locales/sr.json}.
 */
@Service
public class PdfLabelService {

    private static final String SCRIPT_LATIN = "latin";
    private static final String SCRIPT_CYRILLIC = "cyrillic";

    private final Map<String, Map<String, String>> labelsByScript = new HashMap<>();

    public PdfLabelService(ObjectMapper objectMapper) {
        JsonNode pdf = loadPdfRoot(objectMapper, "locales/sr.json");
        labelsByScript.put(SCRIPT_LATIN, readScriptSection(pdf, SCRIPT_LATIN));
        labelsByScript.put(SCRIPT_CYRILLIC, readScriptSection(pdf, SCRIPT_CYRILLIC));
    }

    public String label(String key, boolean roll80) {
        String script = roll80 ? SCRIPT_CYRILLIC : SCRIPT_LATIN;
        Map<String, String> scriptLabels = labelsByScript.get(script);
        if (scriptLabels != null && scriptLabels.containsKey(key)) {
            return scriptLabels.get(key);
        }
        Map<String, String> fallback = labelsByScript.get(SCRIPT_LATIN);
        if (fallback != null && fallback.containsKey(key)) {
            return fallback.get(key);
        }
        return key;
    }

    public String labelWithColon(String key, boolean roll80) {
        String value = label(key, roll80);
        if (value == null || value.isBlank() || value.endsWith(":")) {
            return value;
        }
        return value + ":";
    }

    private static JsonNode loadPdfRoot(ObjectMapper objectMapper, String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        if (!resource.exists()) {
            return objectMapper.createObjectNode();
        }
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readTree(in).path("pdf");
        } catch (IOException ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private static Map<String, String> readScriptSection(JsonNode pdf, String script) {
        Map<String, String> result = new HashMap<>();
        JsonNode section = pdf.path(script);
        if (!section.isObject()) {
            return result;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = section.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue().isTextual()) {
                result.put(field.getKey(), field.getValue().asText());
            }
        }
        return result;
    }
}

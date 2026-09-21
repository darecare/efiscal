package com.efiscal.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * ESIR number of this software installation, built from {@code app.esir-number} and
 * {@code app.software-version} in {@code application.yml}.
 * Used as the PDF "ESIR broj" value and as the Tax Authority {@code invoiceNumber} request field.
 * Unrelated to {@code fiscalbill.efiscal_requestedby}, which stores the Tax Authority response field.
 */
@Service
public class EsirNumberService {

    private final String esirNumber;
    private final String softwareVersion;

    public EsirNumberService(
            @Value("${app.esir-number:}") String esirNumber,
            @Value("${app.software-version:}") String softwareVersion) {
        this.esirNumber = esirNumber;
        this.softwareVersion = softwareVersion;
    }

    /**
     * ESIR number as {@code <esir-number>/<software-version>}, or whichever part is configured.
     * Returns an empty string when neither is configured.
     */
    public String resolveEsirNumber() {
        String esir = trimmed(esirNumber);
        String version = trimmed(softwareVersion);
        if (!esir.isEmpty() && !version.isEmpty()) {
            return esir + "/" + version;
        }
        return esir.isEmpty() ? version : esir;
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }
}

package com.efiscal.backend.controller;

import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.report.FiscalReportModels.CatalogEntry;
import com.efiscal.backend.service.report.FiscalReportModels.ReportPageResult;
import com.efiscal.backend.service.report.FiscalReportRegistry;
import com.efiscal.backend.service.report.FiscalReportRegistry.ExportPayload;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports/fiscal")
public class ReportController {

    private final FiscalReportRegistry fiscalReportRegistry;
    private final AuthorizationService authorizationService;

    public ReportController(FiscalReportRegistry fiscalReportRegistry, AuthorizationService authorizationService) {
        this.fiscalReportRegistry = fiscalReportRegistry;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/catalog")
    public List<CatalogEntry> catalog() {
        authorizationService.requireAction("REPORTS_VIEW");
        return fiscalReportRegistry.catalog();
    }

    @GetMapping
    public ReportPageResult run(
            @RequestParam Long orgId,
            @RequestParam String report,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        authorizationService.requireAction("REPORTS_VIEW");
        authorizationService.requireOrgAccess(orgId);
        return fiscalReportRegistry.run(report, orgId, from, to, page, size);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam Long orgId,
            @RequestParam String report,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        authorizationService.requireAction("REPORTS_VIEW");
        authorizationService.requireOrgAccess(orgId);
        ExportPayload payload = fiscalReportRegistry.export(report, orgId, from, to, lang);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.fileName() + "\"")
                .contentType(new MediaType("text", "csv"))
                .body(payload.body());
    }
}

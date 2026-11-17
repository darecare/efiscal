package com.efiscal.backend.service.report;

import com.efiscal.backend.service.report.FiscalReportModels.CatalogEntry;
import com.efiscal.backend.service.report.FiscalReportModels.ReportPageResult;
import com.efiscal.backend.service.report.ReportDateRange.Bounds;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FiscalReportRegistry {

    private final Map<String, FiscalReportHandler> handlersByCode;

    public FiscalReportRegistry(List<FiscalReportHandler> handlers) {
        Map<String, FiscalReportHandler> map = new LinkedHashMap<>();
        for (FiscalReportHandler handler : handlers) {
            map.put(handler.code(), handler);
        }
        this.handlersByCode = Map.copyOf(map);
    }

    public List<CatalogEntry> catalog() {
        return handlersByCode.values().stream().map(FiscalReportHandler::catalogEntry).toList();
    }

    public ReportPageResult run(String reportCode, Long orgId, String from, String to, int page, int size) {
        FiscalReportHandler handler = require(reportCode);
        Bounds bounds = ReportDateRange.resolve(from, to);
        return handler.query(orgId, bounds.fromInclusive(), bounds.toExclusive(), page, size);
    }

    public ExportPayload export(String reportCode, Long orgId, String from, String to, String lang) {
        FiscalReportHandler handler = require(reportCode);
        Bounds bounds = ReportDateRange.resolve(from, to);
        byte[] body = handler.exportCsv(orgId, bounds.fromInclusive(), bounds.toExclusive(), lang);
        String fileName = handler.csvFileName(orgId, bounds.fromInclusive(), bounds.toExclusive());
        return new ExportPayload(fileName, body);
    }

    private FiscalReportHandler require(String reportCode) {
        if (reportCode == null || reportCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "report is required");
        }
        FiscalReportHandler handler = handlersByCode.get(reportCode);
        if (handler == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown report: " + reportCode);
        }
        return handler;
    }

    public record ExportPayload(String fileName, byte[] body) {}
}

package com.efiscal.backend.service.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class FiscalReportModels {

    private FiscalReportModels() {}

    public record CatalogEntry(String code, String labelKey, String descriptionKey) {}

    public record ColumnDef(String key, String labelKey) {}

    public record ReportFilters(Long orgId, String from, String to) {}

    public record ReportPageResult(
            String report,
            ReportFilters filters,
            int page,
            int size,
            long totalElements,
            List<ColumnDef> columns,
            List<Map<String, Object>> rows
    ) {}

    public record ProductsSoldRow(
            String productKey,
            String name,
            String sku,
            String gtin,
            String productId,
            BigDecimal qtySold,
            BigDecimal qtyRefunded,
            BigDecimal qtyNet,
            BigDecimal amountSold,
            BigDecimal amountRefunded,
            BigDecimal amountNet,
            long billCount
    ) {}
}

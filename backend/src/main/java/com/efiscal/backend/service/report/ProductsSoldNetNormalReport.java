package com.efiscal.backend.service.report;

import com.efiscal.backend.repository.FiscalBillReportRepository;
import com.efiscal.backend.service.report.FiscalReportModels.CatalogEntry;
import com.efiscal.backend.service.report.FiscalReportModels.ColumnDef;
import com.efiscal.backend.service.report.FiscalReportModels.ProductsSoldRow;
import com.efiscal.backend.service.report.FiscalReportModels.ReportFilters;
import com.efiscal.backend.service.report.FiscalReportModels.ReportPageResult;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProductsSoldNetNormalReport implements FiscalReportHandler {

    public static final String CODE = "products-sold-net-normal";

    private static final List<ColumnDef> COLUMNS = List.of(
            new ColumnDef("name", "reports.columns.name"),
            new ColumnDef("sku", "reports.columns.sku"),
            new ColumnDef("gtin", "reports.columns.gtin"),
            new ColumnDef("qtySold", "reports.columns.qtySold"),
            new ColumnDef("qtyRefunded", "reports.columns.qtyRefunded"),
            new ColumnDef("qtyNet", "reports.columns.qtyNet"),
            new ColumnDef("amountSold", "reports.columns.amountSold"),
            new ColumnDef("amountRefunded", "reports.columns.amountRefunded"),
            new ColumnDef("amountNet", "reports.columns.amountNet"),
            new ColumnDef("billCount", "reports.columns.billCount")
    );

    private final FiscalBillReportRepository repository;
    private final int exportMaxRows;

    public ProductsSoldNetNormalReport(
            FiscalBillReportRepository repository,
            @Value("${app.reports.export-max-rows:50000}") int exportMaxRows) {
        this.repository = repository;
        this.exportMaxRows = exportMaxRows;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public CatalogEntry catalogEntry() {
        return new CatalogEntry(
                CODE,
                "reports.catalog.productsSoldNetNormal",
                "reports.catalog.productsSoldNetNormalDescription");
    }

    @Override
    public ReportPageResult query(Long orgId, String fromInclusive, String toExclusive, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 50 : Math.min(size, 200);
        long total = repository.countProductsSoldNetNormal(orgId, fromInclusive, toExclusive);
        int offset = safePage * safeSize;
        List<ProductsSoldRow> data = offset >= total
                ? List.of()
                : repository.findProductsSoldNetNormal(orgId, fromInclusive, toExclusive, offset, safeSize);
        List<Map<String, Object>> rows = data.stream().map(this::toRowMap).toList();
        return new ReportPageResult(
                CODE,
                new ReportFilters(orgId, fromInclusive, toExclusive),
                safePage,
                safeSize,
                total,
                COLUMNS,
                rows);
    }

    @Override
    public byte[] exportCsv(Long orgId, String fromInclusive, String toExclusive, String lang) {
        long total = repository.countProductsSoldNetNormal(orgId, fromInclusive, toExclusive);
        if (total > exportMaxRows) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Export exceeds maximum of " + exportMaxRows + " rows (" + total + " matching)");
        }
        List<ProductsSoldRow> rows = repository.findAllProductsSoldNetNormal(orgId, fromInclusive, toExclusive);
        Locale locale = resolveLocale(lang);
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", COLUMNS.stream().map(c -> csvEscape(headerFor(c, locale))).toList()));
        csv.append('\n');
        for (ProductsSoldRow row : rows) {
            csv.append(csvEscape(nullToEmpty(row.name()))).append(',');
            csv.append(csvEscape(nullToEmpty(row.sku()))).append(',');
            csv.append(csvEscape(nullToEmpty(row.gtin()))).append(',');
            csv.append(formatDecimal(row.qtySold())).append(',');
            csv.append(formatDecimal(row.qtyRefunded())).append(',');
            csv.append(formatDecimal(row.qtyNet())).append(',');
            csv.append(formatDecimal(row.amountSold())).append(',');
            csv.append(formatDecimal(row.amountRefunded())).append(',');
            csv.append(formatDecimal(row.amountNet())).append(',');
            csv.append(row.billCount());
            csv.append('\n');
        }
        return FiscalReportHandler.utf8BomCsv(csv.toString());
    }

    private Map<String, Object> toRowMap(ProductsSoldRow row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("productKey", row.productKey());
        map.put("name", row.name());
        map.put("sku", row.sku());
        map.put("gtin", row.gtin());
        map.put("qtySold", row.qtySold());
        map.put("qtyRefunded", row.qtyRefunded());
        map.put("qtyNet", row.qtyNet());
        map.put("amountSold", row.amountSold());
        map.put("amountRefunded", row.amountRefunded());
        map.put("amountNet", row.amountNet());
        map.put("billCount", row.billCount());
        return map;
    }

    private static Locale resolveLocale(String lang) {
        if (lang != null && lang.toLowerCase(Locale.ROOT).startsWith("sr")) {
            return Locale.forLanguageTag("sr");
        }
        return Locale.ENGLISH;
    }

    private static String headerFor(ColumnDef column, Locale locale) {
        boolean sr = "sr".equals(locale.getLanguage());
        return switch (column.key()) {
            case "name" -> sr ? "Naziv" : "Name";
            case "sku" -> "SKU";
            case "gtin" -> sr ? "EAN/GTIN" : "GTIN";
            case "qtySold" -> sr ? "Količina prodato" : "Qty sold";
            case "qtyRefunded" -> sr ? "Količina refundirano" : "Qty refunded";
            case "qtyNet" -> sr ? "Količina neto" : "Qty net";
            case "amountSold" -> sr ? "Iznos prodato" : "Amount sold";
            case "amountRefunded" -> sr ? "Iznos refundirano" : "Amount refunded";
            case "amountNet" -> sr ? "Iznos neto" : "Amount net";
            case "billCount" -> sr ? "Broj računa" : "Bill count";
            default -> column.key();
        };
    }

    private static String formatDecimal(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}

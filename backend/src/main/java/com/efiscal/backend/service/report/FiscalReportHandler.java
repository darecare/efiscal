package com.efiscal.backend.service.report;

import com.efiscal.backend.service.report.FiscalReportModels.CatalogEntry;
import com.efiscal.backend.service.report.FiscalReportModels.ReportPageResult;
import java.nio.charset.StandardCharsets;

public interface FiscalReportHandler {

    String code();

    CatalogEntry catalogEntry();

    ReportPageResult query(Long orgId, String fromInclusive, String toExclusive, int page, int size);

    /** Full filtered result as UTF-8 CSV bytes (with BOM). Throws if over export cap. */
    byte[] exportCsv(Long orgId, String fromInclusive, String toExclusive, String lang);

    default String csvFileName(Long orgId, String fromInclusive, String toExclusive) {
        String from = sanitizeFilePart(fromInclusive);
        String to = sanitizeFilePart(toExclusive);
        return code() + "_" + orgId + "_" + from + "_" + to + ".csv";
    }

    private static String sanitizeFilePart(String value) {
        if (value == null || value.isBlank()) {
            return "na";
        }
        return value.replaceAll("[^0-9A-Za-zT:+-]", "").replace(':', '-');
    }

    static byte[] utf8BomCsv(String body) {
        byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(content, 0, out, bom.length, content.length);
        return out;
    }
}

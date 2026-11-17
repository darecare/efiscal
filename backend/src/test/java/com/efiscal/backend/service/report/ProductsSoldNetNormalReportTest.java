package com.efiscal.backend.service.report;

import com.efiscal.backend.repository.FiscalBillReportRepository;
import com.efiscal.backend.service.report.FiscalReportModels.ProductsSoldRow;
import com.efiscal.backend.service.report.FiscalReportModels.ReportPageResult;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductsSoldNetNormalReportTest {

    @Mock FiscalBillReportRepository repository;

    private ProductsSoldNetNormalReport report;

    @BeforeEach
    void setUp() {
        report = new ProductsSoldNetNormalReport(repository, 50_000);
    }

    @Test
    void queryMapsNetColumns() {
        ProductsSoldRow row = new ProductsSoldRow(
                "SKU-1", "Widget", "SKU-1", null, null,
                new BigDecimal("10"), new BigDecimal("2"), new BigDecimal("8"),
                new BigDecimal("1000.00"), new BigDecimal("200.00"), new BigDecimal("800.00"),
                3L);
        when(repository.countProductsSoldNetNormal(eq(1L), anyString(), anyString())).thenReturn(1L);
        when(repository.findProductsSoldNetNormal(eq(1L), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(row));

        ReportPageResult page = report.query(1L, "2024-06-01T00:00:00+02:00", "2024-06-02T00:00:00+02:00", 0, 50);

        assertEquals(ProductsSoldNetNormalReport.CODE, page.report());
        assertEquals(1, page.totalElements());
        assertEquals(new BigDecimal("8"), page.rows().get(0).get("qtyNet"));
        assertEquals(new BigDecimal("800.00"), page.rows().get(0).get("amountNet"));
        assertEquals(3L, page.rows().get(0).get("billCount"));
    }

    @Test
    void exportCsvIncludesBomAndHeaders() {
        ProductsSoldRow row = new ProductsSoldRow(
                "SKU-1", "Widget, special", "SKU-1", "123", "P1",
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE,
                new BigDecimal("10.50"), BigDecimal.ZERO, new BigDecimal("10.50"),
                1L);
        when(repository.countProductsSoldNetNormal(anyLong(), anyString(), anyString())).thenReturn(1L);
        when(repository.findAllProductsSoldNetNormal(anyLong(), anyString(), anyString())).thenReturn(List.of(row));

        byte[] csv = report.exportCsv(1L, "a", "b", "en");
        String text = new String(csv, StandardCharsets.UTF_8);
        assertTrue(text.charAt(0) == '\uFEFF' || (csv[0] == (byte) 0xEF && csv[1] == (byte) 0xBB));
        assertTrue(text.contains("Qty sold"));
        assertTrue(text.contains("\"Widget, special\""));
    }

    @Test
    void exportRejectsWhenOverCap() {
        ProductsSoldNetNormalReport capped = new ProductsSoldNetNormalReport(repository, 2);
        when(repository.countProductsSoldNetNormal(anyLong(), anyString(), anyString())).thenReturn(3L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> capped.exportCsv(1L, "a", "b", "en"));
        assertEquals(400, ex.getStatusCode().value());
    }
}

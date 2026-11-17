package com.efiscal.backend.service.report;

import com.efiscal.backend.service.report.FiscalReportModels.CatalogEntry;
import com.efiscal.backend.service.report.FiscalReportModels.ReportPageResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FiscalReportRegistryTest {

    @Test
    void runResolvesDateOnlyAndDelegates() {
        FiscalReportHandler handler = mock(FiscalReportHandler.class);
        when(handler.code()).thenReturn("products-sold-net-normal");
        when(handler.catalogEntry()).thenReturn(new CatalogEntry("products-sold-net-normal", "l", "d"));
        when(handler.query(anyLong(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new ReportPageResult("products-sold-net-normal", null, 0, 50, 0, List.of(), List.of()));

        FiscalReportRegistry registry = new FiscalReportRegistry(List.of(handler));
        registry.run("products-sold-net-normal", 9L, "2024-06-01", "2024-06-01", 0, 50);

        verify(handler).query(
                eq(9L),
                eq("2024-06-01T00:00:00+02:00"),
                eq("2024-06-02T00:00:00+02:00"),
                eq(0),
                eq(50));
    }

    @Test
    void unknownReportReturns400() {
        FiscalReportRegistry registry = new FiscalReportRegistry(List.of());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> registry.run("missing", 1L, "2024-01-01", "2024-01-02", 0, 50));
        assertEquals(400, ex.getStatusCode().value());
    }
}

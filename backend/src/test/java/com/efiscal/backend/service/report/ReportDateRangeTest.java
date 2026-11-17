package com.efiscal.backend.service.report;

import com.efiscal.backend.service.report.ReportDateRange.Bounds;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportDateRangeTest {

    @Test
    void dateOnlyExpandsToBelgradeDayBounds() {
        Bounds bounds = ReportDateRange.resolve("2024-06-01", "2024-06-01");
        assertTrue(bounds.fromInclusive().startsWith("2024-06-01T00:00:00"));
        assertTrue(bounds.fromInclusive().contains("+02:00") || bounds.fromInclusive().contains("+01:00"));
        assertTrue(bounds.toExclusive().startsWith("2024-06-02T00:00:00"));
    }

    @Test
    void rejectsInvertedRange() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ReportDateRange.resolve("2024-06-02", "2024-06-01"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void acceptsIsoInstants() {
        Bounds bounds = ReportDateRange.resolve(
                "2024-06-01T00:00:00+02:00",
                "2024-06-02T00:00:00+02:00");
        assertEquals("2024-06-01T00:00:00+02:00", bounds.fromInclusive());
        assertEquals("2024-06-02T00:00:00+02:00", bounds.toExclusive());
    }
}

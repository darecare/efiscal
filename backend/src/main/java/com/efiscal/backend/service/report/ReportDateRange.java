package com.efiscal.backend.service.report;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Expands Belgrade calendar days (or accepts full ISO instants) into inclusive/exclusive
 * string bounds for lexical comparison against {@code fiscalbill.efiscal_sdcdatetime}.
 */
public final class ReportDateRange {

    public static final ZoneId BELGRADE = ZoneId.of("Europe/Belgrade");
    private static final DateTimeFormatter OFFSET_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private ReportDateRange() {}

    public record Bounds(String fromInclusive, String toExclusive) {}

    public static Bounds resolve(String from, String to) {
        if (from == null || from.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from is required");
        }
        if (to == null || to.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to is required");
        }
        String fromInclusive = normalizeStart(from.trim());
        String toExclusive = normalizeEndExclusive(to.trim());
        if (fromInclusive.compareTo(toExclusive) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
        }
        return new Bounds(fromInclusive, toExclusive);
    }

    private static String normalizeStart(String raw) {
        if (looksLikeDateOnly(raw)) {
            LocalDate day = LocalDate.parse(raw);
            return ZonedDateTime.of(day, LocalTime.MIDNIGHT, BELGRADE).format(OFFSET_FMT);
        }
        return formatInstant(raw);
    }

    /** Date-only {@code to} is inclusive end of that Belgrade day → exclusive next midnight. */
    private static String normalizeEndExclusive(String raw) {
        if (looksLikeDateOnly(raw)) {
            LocalDate day = LocalDate.parse(raw);
            return ZonedDateTime.of(day.plusDays(1), LocalTime.MIDNIGHT, BELGRADE).format(OFFSET_FMT);
        }
        return formatInstant(raw);
    }

    private static boolean looksLikeDateOnly(String raw) {
        return raw.length() == 10 && raw.charAt(4) == '-' && raw.charAt(7) == '-';
    }

    private static String formatInstant(String raw) {
        try {
            return OffsetDateTime.parse(raw).format(OFFSET_FMT);
        } catch (DateTimeParseException ignored) {
            try {
                return ZonedDateTime.parse(raw).toOffsetDateTime().format(OFFSET_FMT);
            } catch (DateTimeParseException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format: " + raw);
            }
        }
    }
}

package com.efiscal.backend.service;

/**
 * Builds Tax Authority / PDF line names for Advance invoices from tax configuration.
 * Format: {@code <advancePrefix> <advanceName> (<taxMark>)}
 */
final class AdvanceLineNameResolver {

    private AdvanceLineNameResolver() {}

    static String format(String advancePrefix, String advanceName, String taxMark) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, advancePrefix);
        appendPart(sb, advanceName);
        if (taxMark != null && !taxMark.isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append('(').append(taxMark.trim()).append(')');
        }
        return sb.toString();
    }

    private static void appendPart(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(part.trim());
    }
}

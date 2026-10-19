package com.efiscal.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AdvanceLineNameResolverTest {

    @Test
    void formatsPrefixNameAndTaxMark() {
        assertEquals("20 Avans (Ђ)", AdvanceLineNameResolver.format("20", "Avans", "Ђ"));
    }

    @Test
    void trimsPartsAndSkipsBlank() {
        assertEquals("Avans (A)", AdvanceLineNameResolver.format("  ", " Avans ", "A"));
        assertEquals("(E)", AdvanceLineNameResolver.format(null, null, "E"));
        assertEquals("20 Avans", AdvanceLineNameResolver.format("20", "Avans", "  "));
    }
}

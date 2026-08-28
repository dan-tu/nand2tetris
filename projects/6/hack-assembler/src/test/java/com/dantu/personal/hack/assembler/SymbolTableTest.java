package com.dantu.personal.hack.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SymbolTableTest {

    private static final int FIRST_VARIABLE = 16;

    /** A table seeded with the real Hack predefined symbols. */
    private static SymbolTable hackTable() {
        return new SymbolTable(PredefinedSymbol.asMap(), FIRST_VARIABLE);
    }

    /** A table with no predefined symbols, for tests that don't need them. */
    private static SymbolTable emptyTable() {
        return new SymbolTable(Map.of(), FIRST_VARIABLE);
    }

    @Nested
    class PredefinedSymbols {

        @Test
        void resolvesToPredefinedAddress() {
            SymbolTable table = hackTable();
            assertEquals(0, table.resolve("SP"));
            assertEquals(4, table.resolve("THAT"));
            assertEquals(15, table.resolve("R15"));
            assertEquals(16384, table.resolve("SCREEN"));
            assertEquals(24576, table.resolve("KBD"));
        }

        @Test
        void aliasesShareAnAddress() {
            SymbolTable table = hackTable();
            assertEquals(table.resolve("R0"), table.resolve("SP"));
            assertEquals(table.resolve("R1"), table.resolve("LCL"));
            assertEquals(table.resolve("R4"), table.resolve("THAT"));
        }

        @Test
        void doNotConsumeVariableAddresses() {
            SymbolTable table = hackTable();
            table.resolve("SCREEN");
            table.resolve("KBD");

            // The first real variable still gets the first variable address.
            assertEquals(FIRST_VARIABLE, table.resolve("counter"));
        }

        @Test
        void initialMapIsCopiedNotAliased() {
            Map<String, Integer> seed = new HashMap<>(Map.of("SP", 0));
            SymbolTable table = new SymbolTable(seed, FIRST_VARIABLE);

            seed.put("SNEAKY", 999);

            // The table took a snapshot; later edits to the caller's map are invisible.
            assertNotEquals(999, table.resolve("SNEAKY"));
        }
    }

    @Nested
    class Labels {

        @Test
        void resolveReturnsTheDefinedAddress() {
            SymbolTable table = emptyTable();
            table.addLabel("LOOP", 42);

            assertEquals(42, table.resolve("LOOP"));
        }

        @Test
        void duplicateLabelThrows() {
            SymbolTable table = emptyTable();
            table.addLabel("LOOP", 42);

            assertThrows(RuntimeException.class, () -> table.addLabel("LOOP", 99));
        }

        @Test
        void doNotConsumeVariableAddresses() {
            SymbolTable table = emptyTable();
            table.addLabel("LOOP", 42);
            table.addLabel("END", 77);

            assertEquals(FIRST_VARIABLE, table.resolve("counter"));
        }

        @Test
        void labelWinsOverVariableAllocation() {
            SymbolTable table = emptyTable();
            table.addLabel("END", 42);

            // @END must resolve to the label, not be allocated as a fresh variable.
            assertEquals(42, table.resolve("END"));
        }
    }

    @Nested
    class Variables {

        @Test
        void firstUnknownSymbolGetsFirstVariableAddress() {
            assertEquals(FIRST_VARIABLE, emptyTable().resolve("i"));
        }

        @Test
        void distinctSymbolsGetSequentialAddresses() {
            SymbolTable table = emptyTable();

            assertEquals(16, table.resolve("i"));
            assertEquals(17, table.resolve("j"));
            assertEquals(18, table.resolve("sum"));
        }

        @Test
        void repeatedResolutionIsStable() {
            SymbolTable table = emptyTable();

            int first = table.resolve("i");
            table.resolve("j");
            int second = table.resolve("i");

            assertEquals(first, second);
            assertEquals(16, first);
        }

        @Test
        void allocationOrderFollowsFirstEncounter() {
            SymbolTable table = emptyTable();

            table.resolve("z");
            table.resolve("a");

            // Order of first appearance, not alphabetical or insertion-sorted.
            assertEquals(16, table.resolve("z"));
            assertEquals(17, table.resolve("a"));
        }
    }
}

package com.dantu.personal.hack.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class BinaryInstructionEncoderTest {
    BinaryInstructionEncoder encoder = new BinaryInstructionEncoder();

    @Nested
    class TestEncodeAInstruction {

        @Test
        void test0() {
            assertEquals("0000000000000000", encoder.encodeAInstruction(0));
        }

        @Test
        void test1024() {
            assertEquals("0000010000000000", encoder.encodeAInstruction((int) Math.pow(2, 10)));
        }

        @Test
        void test32768() {
            assertThrows(IllegalArgumentException.class, () -> encoder.encodeAInstruction((int) Math.pow(2, 15)));
        }
    }

    @Nested
    class TestEncodeDestination {
        @Test
        void testNoDestination() {
            assertEquals("000", encoder.encodeDestination(""));
        }

        @Test
        void testDestinationA() {
            assertEquals("100", encoder.encodeDestination("A"));
        }

        @Test
        void testDestinationM() {
            assertEquals("001", encoder.encodeDestination("M"));
        }

        @Test
        void testDestinationD() {
            assertEquals("010", encoder.encodeDestination("D"));
        }

        @Test
        void testDestinationAM() {
            assertEquals("101", encoder.encodeDestination("AM"));
        }

        @Test
        void testDestinationAD() {
            assertEquals("110", encoder.encodeDestination("AD"));
        }

        @Test
        void testDestinationMD() {
            assertEquals("011", encoder.encodeDestination("MD"));
        }

        @Test
        void testDestinationAMD() {
            assertEquals("111", encoder.encodeDestination("AMD"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "MDA", "test", "1234", "abc.df" })
        void testInvalidDestination(String dest) {
            assertThrows(IllegalArgumentException.class, () -> encoder.encodeDestination(dest));
        }
    }

    @Nested
    class TestEncodeComputation {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                // constants
                "0,    0101010",
                "1,    0111111",
                "-1,   0111010",
                // a = 0: operate on A
                "D,    0001100",
                "A,    0110000",
                "!D,   0001101",
                "!A,   0110001",
                "-D,   0001111",
                "-A,   0110011",
                "D+1,  0011111",
                "A+1,  0110111",
                "D-1,  0001110",
                "A-1,  0110010",
                "D+A,  0000010",
                "D-A,  0010011",
                "A-D,  0000111",
                "D&A,  0000000",
                "D|A,  0010101",
                // a = 1: same control bits, operating on M
                "M,    1110000",
                "!M,   1110001",
                "-M,   1110011",
                "M+1,  1110111",
                "M-1,  1110010",
                "D+M,  1000010",
                "D-M,  1010011",
                "M-D,  1000111",
                "D&M,  1000000",
                "D|M,  1010101",
        })
        void encodesEveryLegalComputation(String comp, String expected) {
            assertEquals(expected, encoder.encodeComputation(comp));
            assertEquals(7, encoder.encodeComputation(comp).length(), comp);
        }

        @Test
        void aBitDistinguishesAFromM() {
            // The six ALU control bits are identical; only the leading 'a' bit differs.
            assertEquals(encoder.encodeComputation("A").substring(1),
                    encoder.encodeComputation("M").substring(1));
            assertEquals('0', encoder.encodeComputation("A").charAt(0));
            assertEquals('1', encoder.encodeComputation("M").charAt(0));
        }

        @ParameterizedTest
        @ValueSource(strings = { "A+D", "M+D", "D+2", "A&D", "1+1", "Q", "d", "", "D  +A" })
        void invalidComputationThrows(String comp) {
            assertThrows(IllegalArgumentException.class, () -> encoder.encodeComputation(comp));
        }
    }

    @Nested
    class TestEncodeJump {

        @Test
        void emptyJumpEncodesToZeros() {
            assertEquals("000", encoder.encodeJump(""));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "JGT, 001",
                "JEQ, 010",
                "JGE, 011",
                "JLT, 100",
                "JNE, 101",
                "JLE, 110",
                "JMP, 111",
        })
        void encodesEveryJumpMnemonic(String jump, String expected) {
            assertEquals(expected, encoder.encodeJump(jump));
        }

        @ParameterizedTest
        @ValueSource(strings = { "JGZ", "JZ", "jmp", "JUMP", "J", "0", "JMPJMP", "JMP " })
        void invalidJumpThrows(String jump) {
            assertThrows(IllegalArgumentException.class, () -> encoder.encodeJump(jump));
        }
    }

    @Nested
    class TestEncodeCInstruction {

        @ParameterizedTest(name = "{0}={1};{2} -> {3}")
        @CsvSource({
                // dest, comp, jump, expected -- vectors from the course's Add.asm / Max.asm
                "D,  A,    '',   1110110000010000",
                "D,  D+A,  '',   1110000010010000",
                "M,  D,    '',   1110001100001000",
                "'', 0,    JMP,  1110101010000111",
                "'', D,    JGT,  1110001100000001",
                "D,  M,    '',   1111110000010000",
                "'', D-M,  JLE,  1111010011000110",
                "AMD, M+1, JNE,  1111110111111101",
        })
        void encodesFullCInstruction(String dest, String comp, String jump, String expected) {
            assertEquals(expected, encoder.encodeCInstruction(dest, comp, jump));
        }

        @Test
        void alwaysSixteenBitsStartingWith111() {
            String encoded = encoder.encodeCInstruction("AMD", "D|M", "JMP");
            assertEquals(16, encoded.length());
            assertEquals("111", encoded.substring(0, 3));
        }

        @Test
        void fieldsAppearInCompDestJumpOrder() {
            String encoded = encoder.encodeCInstruction("AD", "D-1", "JLT");
            assertEquals(encoder.encodeComputation("D-1"), encoded.substring(3, 10));
            assertEquals(encoder.encodeDestination("AD"), encoded.substring(10, 13));
            assertEquals(encoder.encodeJump("JLT"), encoded.substring(13, 16));
        }

        @Test
        void nullDestAndJumpAreTreatedAsAbsent() {
            assertEquals(encoder.encodeCInstruction("", "D", ""),
                    encoder.encodeCInstruction(null, "D", null));
        }

        @Test
        void invalidFieldThrows() {
            assertThrows(IllegalArgumentException.class, () -> encoder.encodeCInstruction("Q", "D", ""));
            assertThrows(IllegalArgumentException.class, () -> encoder.encodeCInstruction("D", "D+2", ""));
            assertThrows(IllegalArgumentException.class, () -> encoder.encodeCInstruction("D", "D", "JZ"));
        }
    }
}

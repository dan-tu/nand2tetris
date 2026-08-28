package com.dantu.personal.hack.assembler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ParserTest {

    @Nested
    class TestParse {

        @Nested
        class ParseAInstruction {
            @Test
            void simpleSymbol() {
                String instruction = "@abcdefg";
                Instruction result = Parser.parse(instruction);
                AInstruction a = assertInstanceOf(AInstruction.class, result);
                assertEquals("abcdefg", a.value());
            }

            @Test
            void validConstant() {
                String instruction = "@2765";
                Instruction result = Parser.parse(instruction);
                AInstruction a = assertInstanceOf(AInstruction.class, result);
                assertEquals("2765", a.value());
            }

            @Test
            void invalidConstant() {
                String instruction = "@27.65";
                assertThrows(SyntaxException.class, () -> Parser.parse(instruction));
            }

            @Test
            void validCharacters() {
                String instruction = "@a_bc.de$$:sfdsa331";
                Instruction result = Parser.parse(instruction);
                AInstruction a = assertInstanceOf(AInstruction.class, result);
                assertEquals("a_bc.de$$:sfdsa331", a.value());
            }

            @Test
            void invalidConstantThrowsException() {
                String instruction = "@-24.5";
                assertThrows(SyntaxException.class, () -> Parser.parse(instruction));
            }

            @Test
            void invalidCharacterThrowsException() {
                String instruction = "@&&&&";
                assertThrows(SyntaxException.class, () -> Parser.parse(instruction));
            }

            @Test
            void emptyValue() {
                String instruction = "@";
                assertThrows(SyntaxException.class, () -> Parser.parse(instruction));
            }
        }

        @Nested
        class ParseLInstruction {
            @Test
            void parseSimpleLabel() {
                String instruction = "(LOOP)";
                Instruction result = Parser.parse(instruction);
                LInstruction l = assertInstanceOf(LInstruction.class, result);
                assertEquals("LOOP", l.label());
            }

            @Test
            void supportedCharactersOkay() {
                String instruction = "(a_bc.de$$:sfdsa331)";
                Instruction result = Parser.parse(instruction);
                LInstruction l = assertInstanceOf(LInstruction.class, result);
                assertEquals("a_bc.de$$:sfdsa331", l.label());
            }

            @Test
            void numberThrowsException() {
                String instruction = "(12345)";
                assertThrows(SyntaxException.class, () -> Parser.parse(instruction));
            }

            @Test
            void negativeNumberThrowsException() {
                String instruction = "(-12345)";
                assertThrows(SyntaxException.class, () -> Parser.parse(instruction));
            }

            @Test
            void unsupportedCharacterThrowsException() {
                String instruction = "(&&&&&)";
                assertThrows(SyntaxException.class, () -> Parser.parse(instruction));
            }
        }

        @Nested
        class ParseCInstruction {

            @Test
            void onlyComp() {
                assertEquals(new CInstruction("", "D", ""), Parser.parse("D"));
            }

            @Test
            void destAndComp() {
                assertEquals(new CInstruction("AD", "M+1", ""), Parser.parse("AD=M+1"));
            }

            @Test
            void compAndJump() {
                assertEquals(new CInstruction("", "D", "JEQ"), Parser.parse("D;JEQ"));
            }

            @Test
            void destCompJump() {
                assertEquals(new CInstruction("D", "!M", "JEQ"), Parser.parse("D=!M;JEQ"));
            }

            @Test
            void doubleEqualSignThrowsException() {
                assertThrows(SyntaxException.class, () -> Parser.parse("D==M"));
            }

            @Test
            void doubleSemicolonThrowsException() {
                assertThrows(SyntaxException.class, () -> Parser.parse("D=M;;JEQ"));
            }

            @Test
            void hangingEqualSignThrowsException() {
                assertThrows(SyntaxException.class, () -> Parser.parse("D="));
            }

            @Test
            void hangingSemicolonThrowsException() {
                assertThrows(SyntaxException.class, () -> Parser.parse("D=M;"));
            }

            @Test
            void endsWithSemicolonThrowsException() {
                assertThrows(SyntaxException.class, () -> Parser.parse("D=M;JEQ;"));
            }

            @Test
            void endsWithEqualsThrowsException() {
                assertThrows(SyntaxException.class, () -> Parser.parse("D=M;JEQ="));
            }

            @Test
            void equalSignAndSemicolonSwappedThrowsException() {
                assertThrows(SyntaxException.class, () -> Parser.parse("D;M=JEQ"));
            }

            @Test
            void onlySemicolonThrowsException() {
                assertThrows(SyntaxException.class, () -> Parser.parse(";"));
            }

            @Test
            void onlyEqualsThrowsException() {
                assertThrows(SyntaxException.class, () -> Parser.parse("="));
            }
        }
    }

    @Nested
    class TestNormalize {
        @Test
        void normalizeValidInstructionReturnsInstruction() {
            String s = "@abcdefg";
            assertEquals(s, Parser.normalize(s));
        }

        @Test
        void stripsLeadingTrailingWhitespace() {
            String s = "      @abcdefg        ";
            assertEquals("@abcdefg", Parser.normalize(s));
        }

        @Test
        void stripsWhitespaceInBetween() {
            String s = " D = M + 2     ";
            assertEquals("D=M+2", Parser.normalize(s));
        }

        @Test
        void stripsComments() {
            String s = " D = M + 2     // This does something";
            assertEquals("D=M+2", Parser.normalize(s));
        }

        @Test
        void commentOnly() {
            String s = " // My comment line";
            assertEquals("", Parser.normalize(s));
        }
    }

    @Nested
    class TestHasMoreCommands {
        @Test
        void emptyStream_returnsFalse() throws Exception {
            String empty = "";
            ByteArrayInputStream input = new ByteArrayInputStream(empty.getBytes(StandardCharsets.UTF_8));
            try (Parser p = new Parser(input)) {
                assertFalse(p.hasMoreCommands());
            }
        }

        @Test
        void singleLine_returnsTrue() throws Exception {
            String s = "abcdefg";
            ByteArrayInputStream input = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
            try (Parser p = new Parser(input)) {
                assertTrue(p.hasMoreCommands());
            }
        }

        @Test
        void multipleCalls_doesNotConsumeNextLine() throws Exception {
            CountingReader reader = CountingReader.of("abcdefg");
            try (Parser p = new Parser(reader)) {
                p.hasMoreCommands();
                p.hasMoreCommands();
            }
            assertEquals(1, reader.getLineCalls());
        }

        @Test
        void ignoresWhitespace() throws Exception {
            String s = "\r\n     \r\nabcdefg";
            CountingReader reader = CountingReader.of(s);
            try (Parser p = new Parser(reader)) {
                p.hasMoreCommands();
            }
            assertEquals(3, reader.getLineCalls());
        }

        @Test
        void ignoresComments() throws Exception {
            String s = "// This is my comment\r\nabcdefg";
            CountingReader reader = CountingReader.of(s);
            try (Parser p = new Parser(reader)) {
                p.hasMoreCommands();
            }
            assertEquals(2, reader.getLineCalls());
        }

        static class CountingReader extends BufferedReader {
            private int readLineCalls = 0;

            static CountingReader of(String s) {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
                InputStreamReader iStreamReader = new InputStreamReader(inputStream);
                return new CountingReader(iStreamReader);
            }

            CountingReader(Reader in) {
                super(in);
            }

            @Override
            public String readLine() throws IOException {
                readLineCalls++;
                return super.readLine();
            }

            int getLineCalls() {
                return readLineCalls;
            }
        }
    }

    @Test
    void testMultiLineInstructions() throws Exception {
        String program = """
                (MAIN)
                  @2
                  D=A
                  @3
                  D=D+A
                  @R0
                  M=D
                  @MAIN
                  D;JGT
                """;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(program.getBytes());
        try (Parser p = new Parser(inputStream)) {
            assertTrue(p.hasMoreCommands());
            assertDoesNotThrow(() -> p.advance());
            LInstruction l = assertInstanceOf(LInstruction.class, p.getInstruction());
            assertEquals("MAIN", l.label());

            assertTrue(p.hasMoreCommands());
            assertDoesNotThrow(() -> p.advance());
            AInstruction a = assertInstanceOf(AInstruction.class, p.getInstruction());
            assertEquals("2", a.value());

            assertTrue(p.hasMoreCommands());
            assertDoesNotThrow(() -> p.advance());
            CInstruction c = assertInstanceOf(CInstruction.class, p.getInstruction());
            assertEquals(new CInstruction("D", "A", ""), c);

            assertTrue(p.hasMoreCommands());
            assertDoesNotThrow(() -> p.advance());
            a = assertInstanceOf(AInstruction.class, p.getInstruction());
            assertEquals("3", a.value());

            assertTrue(p.hasMoreCommands());
            assertDoesNotThrow(() -> p.advance());
            c = assertInstanceOf(CInstruction.class, p.getInstruction());
            assertEquals(new CInstruction("D", "D+A", ""), c);

            assertTrue(p.hasMoreCommands());
            assertDoesNotThrow(() -> p.advance());
            a = assertInstanceOf(AInstruction.class, p.getInstruction());
            assertEquals("R0", a.value());

            assertTrue(p.hasMoreCommands());
            assertDoesNotThrow(() -> p.advance());
            c = assertInstanceOf(CInstruction.class, p.getInstruction());
            assertEquals(new CInstruction("M", "D", ""), c);

            assertTrue(p.hasMoreCommands());
            assertDoesNotThrow(() -> p.advance());
            a = assertInstanceOf(AInstruction.class, p.getInstruction());
            assertEquals("MAIN", a.value());

            assertTrue(p.hasMoreCommands());
            assertDoesNotThrow(() -> p.advance());
            c = assertInstanceOf(CInstruction.class, p.getInstruction());
            assertEquals(new CInstruction("", "D", "JGT"), c);
        }
    }
}

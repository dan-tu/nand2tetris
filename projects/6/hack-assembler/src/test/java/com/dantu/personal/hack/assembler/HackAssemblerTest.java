package com.dantu.personal.hack.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HackAssemblerTest {

    private static Path resource(String name) throws URISyntaxException {
        return Path.of(HackAssemblerTest.class.getResource("/" + name).toURI());
    }

    /** Reads a .hack file as a newline-separated string, ignoring a trailing newline. */
    private static String expectedOutput(String name) throws IOException, URISyntaxException {
        return String.join("\n", Files.readAllLines(resource(name)));
    }

    @Test
    void assemblesProgToMatchProgHack() throws Exception {
        String actual = new HackAssembler().assemble(resource("Prog.asm"));

        assertEquals(expectedOutput("Prog.hack"), actual);
    }

    @Test
    void everyOutputLineIsSixteenBits() throws Exception {
        String actual = new HackAssembler().assemble(resource("Prog.asm"));

        for (String line : actual.split("\n")) {
            assertTrue(line.matches("[01]{16}"), "not a 16-bit word: " + line);
        }
    }

    @Test
    void labelsAndBlankLinesProduceNoOutput() throws Exception {
        String actual = new HackAssembler().assemble(resource("Prog.asm"));

        // Prog.asm has 41 lines but only 26 code-generating instructions.
        assertEquals(26, actual.split("\n").length);
    }

    @Test
    void resolvesForwardAndBackwardLabels(@TempDir Path dir) throws Exception {
        Path asm = dir.resolve("Jumps.asm");
        Files.writeString(asm, """
                @FORWARD    // defined later in the file
                0;JMP
                (BACK)
                @BACK       // defined earlier in the file
                0;JMP
                (FORWARD)
                @BACK
                0;JMP
                """);

        assertEquals(String.join("\n", List.of(
                "0000000000000100",   // @FORWARD -> ROM 4
                "1110101010000111",
                "0000000000000010",   // @BACK    -> ROM 2
                "1110101010000111",
                "0000000000000010",   // @BACK
                "1110101010000111")),
                new HackAssembler().assemble(asm));
    }

    @Test
    void variablesAllocateFromSixteenInFirstEncounterOrder(@TempDir Path dir) throws Exception {
        Path asm = dir.resolve("Vars.asm");
        Files.writeString(asm, """
                @zebra
                M=1
                @apple
                M=1
                @zebra
                M=0
                """);

        assertEquals(String.join("\n", List.of(
                "0000000000010000",   // zebra -> 16 (first encountered)
                "1110111111001000",
                "0000000000010001",   // apple -> 17
                "1110111111001000",
                "0000000000010000",   // zebra -> 16 again, stable
                "1110101010001000")),
                new HackAssembler().assemble(asm));
    }

    @Test
    void predefinedSymbolsDoNotConsumeVariableAddresses(@TempDir Path dir) throws Exception {
        Path asm = dir.resolve("Predef.asm");
        Files.writeString(asm, """
                @SCREEN
                @KBD
                @SP
                @counter
                """);

        assertEquals(String.join("\n", List.of(
                "0100000000000000",   // SCREEN = 16384
                "0110000000000000",   // KBD    = 24576
                "0000000000000000",   // SP     = 0
                "0000000000010000")), // counter still gets 16
                new HackAssembler().assemble(asm));
    }

    @Test
    void emptySourceProducesEmptyOutput(@TempDir Path dir) throws Exception {
        Path asm = dir.resolve("Empty.asm");
        Files.writeString(asm, """
                // nothing but comments

                        // and whitespace
                """);

        assertEquals("", new HackAssembler().assemble(asm));
    }

    @Test
    void duplicateLabelIsRejected(@TempDir Path dir) throws Exception {
        Path asm = dir.resolve("Dup.asm");
        Files.writeString(asm, """
                (LOOP)
                @LOOP
                (LOOP)
                0;JMP
                """);

        assertThrows(RuntimeException.class, () -> new HackAssembler().assemble(asm));
    }

    @Test
    void malformedInstructionIsRejected(@TempDir Path dir) throws Exception {
        Path asm = dir.resolve("Bad.asm");
        Files.writeString(asm, """
                @2
                D=Q+1
                """);

        assertThrows(RuntimeException.class, () -> new HackAssembler().assemble(asm));
    }

    @Test
    void missingFileThrowsIOException(@TempDir Path dir) {
        assertThrows(IOException.class, () -> new HackAssembler().assemble(dir.resolve("nope.asm")));
    }
}

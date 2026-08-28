package com.dantu.personal.hack.assembler;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

class HackAssembler {
    private final SymbolTable symbolTable;

    private static SymbolTable initHackSymbolTable() {
        Map<String, Integer> symbols = new HashMap<>(PredefinedSymbol.asMap());
        return new SymbolTable(symbols, 16);
    }

    HackAssembler() {
        this.symbolTable = HackAssembler.initHackSymbolTable();
    }

    /**
     * Assembles the Hack source at {@code source} into machine code.
     *
     * @return the 16-bit instruction words, one per line, newline separated
     */
    String assemble(Path source) throws IOException {
        firstPass(source);
        return secondPass(source);
    }

    /** Records the ROM address of every label. */
    private void firstPass(Path source) throws IOException {
        int nextInstructionAddress = 0;
        try (BufferedReader reader = Files.newBufferedReader(source); Parser parser = new Parser(reader)) {
            while (parser.hasMoreCommands()) {
                parser.advance();
                if (parser.getInstruction() instanceof LInstruction l) {
                    symbolTable.addLabel(l.label(), nextInstructionAddress);
                } else {
                    // Every non-label line produces exactly one instruction.
                    nextInstructionAddress++;
                }
            }
        }
    }

    /** Resolves symbols and encodes each instruction. */
    private String secondPass(Path source) throws IOException {
        BinaryInstructionEncoder encoder = new BinaryInstructionEncoder();
        StringJoiner output = new StringJoiner("\n");

        try (BufferedReader reader = Files.newBufferedReader(source); Parser parser = new Parser(reader)) {
            while (parser.hasMoreCommands()) {
                parser.advance();
                Instruction instruction = parser.getInstruction();
                if (instruction instanceof AInstruction a) {
                    int address = a.isSymbol() ? symbolTable.resolve(a.value()) : Integer.parseInt(a.value());
                    output.add(encoder.encodeAInstruction(address));
                } else if (instruction instanceof CInstruction c) {
                    output.add(encoder.encodeCInstruction(c.dest(), c.comp(), c.jump()));
                }
                // L-instructions emit no code.
            }
        }
        return output.toString();
    }
}

enum PredefinedSymbol {
    SP(0),
    LCL(1),
    ARG(2),
    THIS(3),
    THAT(4),
    R0(0),
    R1(1),
    R2(2),
    R3(3),
    R4(4),
    R5(5),
    R6(6),
    R7(7),
    R8(8),
    R9(9),
    R10(10),
    R11(11),
    R12(12),
    R13(13),
    R14(14),
    R15(15),
    SCREEN(16384),
    KBD(24576);

    final int address;

    PredefinedSymbol(int address) {
        this.address = address;
    }

    int address() {
        return address;
    }

    static Map<String, Integer> asMap() {
        return Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
                PredefinedSymbol::name,
                PredefinedSymbol::address));
    }
}

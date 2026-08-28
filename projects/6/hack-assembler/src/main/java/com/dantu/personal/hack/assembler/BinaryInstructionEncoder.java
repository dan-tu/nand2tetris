package com.dantu.personal.hack.assembler;

import java.util.Map;

/**
 * Encodes {@code Instruction}s as bits.
 */
class BinaryInstructionEncoder {
    private static final Map<String, String> destinations;
    private static final Map<String, String> computations;
    private static final Map<String, String> jumps;

    static {
        destinations = Map.of(
                "", "000",
                "M", "001",
                "D", "010",
                "MD", "011",
                "A", "100",
                "AM", "101",
                "AD", "110",
                "AMD", "111");

        // The 'a' bit selects A (0) or M (1); the remaining six are the ALU
        // control bits zx, nx, zy, ny, f, no.
        computations = Map.ofEntries(
                // constants
                Map.entry("0", "0101010"),
                Map.entry("1", "0111111"),
                Map.entry("-1", "0111010"),
                // a = 0: operate on A
                Map.entry("D", "0001100"),
                Map.entry("A", "0110000"),
                Map.entry("!D", "0001101"),
                Map.entry("!A", "0110001"),
                Map.entry("-D", "0001111"),
                Map.entry("-A", "0110011"),
                Map.entry("D+1", "0011111"),
                Map.entry("A+1", "0110111"),
                Map.entry("D-1", "0001110"),
                Map.entry("A-1", "0110010"),
                Map.entry("D+A", "0000010"),
                Map.entry("D-A", "0010011"),
                Map.entry("A-D", "0000111"),
                Map.entry("D&A", "0000000"),
                Map.entry("D|A", "0010101"),
                // a = 1: same six control bits, operating on M
                Map.entry("M", "1110000"),
                Map.entry("!M", "1110001"),
                Map.entry("-M", "1110011"),
                Map.entry("M+1", "1110111"),
                Map.entry("M-1", "1110010"),
                Map.entry("D+M", "1000010"),
                Map.entry("D-M", "1010011"),
                Map.entry("M-D", "1000111"),
                Map.entry("D&M", "1000000"),
                Map.entry("D|M", "1010101"));

        // j1 j2 j3 test out < 0, out == 0, out > 0 respectively.
        jumps = Map.of(
                "", "000",
                "JGT", "001",
                "JEQ", "010",
                "JGE", "011",
                "JLT", "100",
                "JNE", "101",
                "JLE", "110",
                "JMP", "111");
    }

    BinaryInstructionEncoder() {
    }

    String encodeAInstruction(int address) {
        if (address >= Math.pow(2, 15)) {
            throw new IllegalArgumentException(String.format("Memory address out of bounds: %d", address));
        }

        StringBuilder builder = new StringBuilder(15);
        for (int i = 14; i >= 0; i--) {
            builder.append((address >> i) & 1);
        }
        return "0" + builder.toString();
    }

    /**
     * Encodes a C-instruction as {@code 111 a cccccc ddd jjj}.
     *
     * @param dest destination mnemonic, or "" / null for no destination
     * @param comp computation mnemonic; required
     * @param jump jump mnemonic, or "" / null for no jump
     */
    String encodeCInstruction(String dest, String comp, String jump) {
        return "111"
                + encodeComputation(comp)
                + encodeDestination(dest == null ? "" : dest)
                + encodeJump(jump == null ? "" : jump);
    }

    String encodeDestination(String dest) {
        if (!destinations.containsKey(dest)) {
            throw new IllegalArgumentException(String.format("%s is not a valid destination", dest));
        }
        return destinations.get(dest);
    }

    String encodeComputation(String comp) {
        if (!computations.containsKey(comp)) {
            throw new IllegalArgumentException(String.format("%s is not a valid computation", comp));
        }
        return computations.get(comp);
    }

    String encodeJump(String jump) {
        if (!jumps.containsKey(jump)) {
            throw new IllegalArgumentException(String.format("%s is not a valid jump", jump));
        }
        return jumps.get(jump);
    }
}

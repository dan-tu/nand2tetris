package com.dantu.personal.hack.assembler;

sealed interface Instruction {
}

record AInstruction(String value) implements Instruction {
}

record LInstruction(String label) implements Instruction {
}

record CInstruction(String dest, String comp, String jump) implements Instruction {
}

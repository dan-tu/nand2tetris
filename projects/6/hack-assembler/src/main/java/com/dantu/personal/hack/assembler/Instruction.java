package com.dantu.personal.hack.assembler;

sealed interface Instruction {
}

record AInstruction(String value) implements Instruction {
    boolean isSymbol() {
        return !isConstant();
    }

    boolean isConstant() {
        return Character.isDigit(value.charAt(0));
    }
}

record LInstruction(String label) implements Instruction {
}

record CInstruction(String dest, String comp, String jump) implements Instruction {
}

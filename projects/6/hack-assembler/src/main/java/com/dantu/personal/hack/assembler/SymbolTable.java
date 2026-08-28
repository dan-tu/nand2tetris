package com.dantu.personal.hack.assembler;

import java.util.HashMap;
import java.util.Map;

class SymbolTable {
    private Map<String, Integer> symbols;
    private int nextVariableAddress;

    SymbolTable(Map<String, Integer> initialSymbols, int firstVariableAddress) {
        this.symbols = new HashMap<>(initialSymbols);
        this.nextVariableAddress = firstVariableAddress;
    }

    void addLabel(String label, int address) {
        if (symbols.containsKey(label)) {
            throw new RuntimeException(String.format("Label %s already exists.", label));
        }
        symbols.put(label, address);
    }

    int resolve(String symbol) {
        // If we're resolving a symbol, we're in the second pass
        // of the parser. Any symbol that is not already in
        // the SymbolTable is a variable.
        return symbols.computeIfAbsent(symbol, key -> nextVariableAddress++);
    }
}

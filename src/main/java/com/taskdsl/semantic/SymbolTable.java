package com.taskdsl.semantic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Tabela de símbolos com escopo em pilha (uma pilha de mapas).
 * Cada bloco {@code { ... }} (if/while/for) abre um novo escopo; ao sair
 * do bloco o escopo é descartado. Isso garante que uma variável declarada
 * dentro de um bloco não "vaze" para fora dele.
 */
public class SymbolTable {

    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

    public SymbolTable() {
        enterScope(); // escopo global
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        scopes.pop();
    }

    /**
     * Declara um novo símbolo no escopo atual.
     * @return false se já existir uma variável com o mesmo nome no escopo atual (redeclaração).
     */
    public boolean declare(Symbol symbol) {
        Map<String, Symbol> current = scopes.peek();
        if (current.containsKey(symbol.name)) {
            return false;
        }
        current.put(symbol.name, symbol);
        return true;
    }

    /** Procura o símbolo do escopo mais interno para o mais externo. */
    public Symbol resolve(String name) {
        for (Map<String, Symbol> scope : scopes) {
            Symbol s = scope.get(name);
            if (s != null) return s;
        }
        return null;
    }

    public boolean isDeclared(String name) {
        return resolve(name) != null;
    }
}

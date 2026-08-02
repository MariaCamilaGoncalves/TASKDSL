package com.taskdsl.interpreter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** Guarda os valores das variáveis em tempo de execução, com escopo em pilha. */
public class Environment {

    private final Deque<Map<String, Object>> scopes = new ArrayDeque<>();

    public Environment() {
        enterScope();
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        scopes.pop();
    }

    public void declare(String name, Object value) {
        scopes.peek().put(name, value);
    }

    public void assign(int line, String name, Object value) {
        for (Map<String, Object> scope : scopes) {
            if (scope.containsKey(name)) {
                scope.put(name, value);
                return;
            }
        }
        // Não deveria acontecer: a análise semântica garante que a variável existe.
        throw new RuntimeError(line, "variável '" + name + "' não foi declarada");
    }

    public Object get(int line, String name) {
        for (Map<String, Object> scope : scopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        throw new RuntimeError(line, "variável '" + name + "' não foi declarada");
    }
}

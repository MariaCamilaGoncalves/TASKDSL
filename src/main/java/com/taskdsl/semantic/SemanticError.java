package com.taskdsl.semantic;

public class SemanticError {
    public final int line;
    public final String message;

    public SemanticError(int line, String message) {
        this.line = line;
        this.message = message;
    }

    @Override
    public String toString() {
        return "[Erro semântico] linha " + line + ": " + message;
    }
}

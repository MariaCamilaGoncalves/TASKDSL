package com.taskdsl.interpreter;

public class RuntimeError extends RuntimeException {
    public final int line;

    public RuntimeError(int line, String message) {
        super(message);
        this.line = line;
    }
}

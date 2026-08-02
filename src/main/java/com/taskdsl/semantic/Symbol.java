package com.taskdsl.semantic;

import com.taskdsl.ast.Type;

/** Representa uma variável declarada: nome, tipo e linha de declaração. */
public class Symbol {
    public final String name;
    public final Type type;
    public final int declaredAtLine;

    public Symbol(String name, Type type, int declaredAtLine) {
        this.name = name;
        this.type = type;
        this.declaredAtLine = declaredAtLine;
    }
}

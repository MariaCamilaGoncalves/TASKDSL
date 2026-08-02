package com.taskdsl.ast;

/**
 * Nó base para todas as expressões da linguagem.
 * O campo "resolvedType" é preenchido pelo SemanticAnalyzer durante a
 * checagem de tipos e reaproveitado pelo Interpreter.
 */
public abstract class Expr extends Node {
    public Type resolvedType = Type.UNKNOWN;

    protected Expr(int line) {
        super(line);
    }

    public static final class IntLiteral extends Expr {
        public final int value;
        public IntLiteral(int value, int line) { super(line); this.value = value; }
    }

    public static final class FloatLiteral extends Expr {
        public final double value;
        public FloatLiteral(double value, int line) { super(line); this.value = value; }
    }

    public static final class StringLiteral extends Expr {
        public final String value;
        public StringLiteral(String value, int line) { super(line); this.value = value; }
    }

    public static final class BoolLiteral extends Expr {
        public final boolean value;
        public BoolLiteral(boolean value, int line) { super(line); this.value = value; }
    }

    public static final class VarRef extends Expr {
        public final String name;
        public VarRef(String name, int line) { super(line); this.name = name; }
    }

    public static final class Unary extends Expr {
        public final String op; // '-' ou '!'
        public final Expr operand;
        public Unary(String op, Expr operand, int line) { super(line); this.op = op; this.operand = operand; }
    }

    public static final class Binary extends Expr {
        public final String op; // + - * / % < > <= >= == != && ||
        public final Expr left;
        public final Expr right;
        public Binary(String op, Expr left, Expr right, int line) {
            super(line);
            this.op = op;
            this.left = left;
            this.right = right;
        }
    }
}

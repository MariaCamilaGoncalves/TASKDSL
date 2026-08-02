package com.taskdsl.ast;

/**
 * Tipos reconhecidos pelo analisador semântico.
 *
 * INT, FLOAT e STRING são os tipos primitivos declaráveis pelo usuário
 * (requisito obrigatório da linguagem). BOOL é um tipo interno, resultado
 * de expressões relacionais/lógicas (não pode ser declarado em variáveis).
 * UNKNOWN é usado internamente quando um erro semântico já foi reportado,
 * para evitar uma cascata de erros repetidos.
 */
public enum Type {
    INT,
    FLOAT,
    STRING,
    BOOL,
    UNKNOWN;

    public boolean isNumeric() {
        return this == INT || this == FLOAT;
    }

    public String toDisplay() {
        return name().toLowerCase();
    }
}

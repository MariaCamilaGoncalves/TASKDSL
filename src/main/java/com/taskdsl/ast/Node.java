package com.taskdsl.ast;

/**
 * Classe base de todos os nós da Árvore Sintática Abstrata (AST).
 * Guarda a linha do código-fonte onde o nó foi originado, usada
 * para mensagens de erro semântico e de execução.
 */
public abstract class Node {
    public final int line;

    protected Node(int line) {
        this.line = line;
    }
}

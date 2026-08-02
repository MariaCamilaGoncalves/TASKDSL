package com.taskdsl.errors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener customizado usado tanto pelo Lexer quanto pelo Parser gerados
 * pelo ANTLR. Substitui a saída padrão (que vai para stderr sem controle)
 * por uma lista de erros formatados, sempre indicando a linha do problema,
 * conforme exigido pela Análise Léxica e Análise Sintática.
 */
public class CompilerErrorListener extends BaseErrorListener {

    public static final class ErrorInfo {
        public final int line;
        public final int column;
        public final String message;

        public ErrorInfo(int line, int column, String message) {
            this.line = line;
            this.column = column;
            this.message = message;
        }

        @Override
        public String toString() {
            return "linha " + line + ":" + column + " -> " + message;
        }
    }

    private final String phase; // "léxico" ou "sintático"
    private final List<ErrorInfo> errors = new ArrayList<>();

    public CompilerErrorListener(String phase) {
        this.phase = phase;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                             int line, int charPositionInLine,
                             String msg, RecognitionException e) {
        errors.add(new ErrorInfo(line, charPositionInLine, msg));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<ErrorInfo> getErrors() {
        return errors;
    }

    public void printErrors() {
        for (ErrorInfo err : errors) {
            System.err.println("[Erro " + phase + "] " + err);
        }
    }
}

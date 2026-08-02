package com.taskdsl;

import com.taskdsl.ast.Stmt;
import com.taskdsl.errors.CompilerErrorListener;
import com.taskdsl.interpreter.Interpreter;
import com.taskdsl.interpreter.RuntimeError;
import com.taskdsl.semantic.SemanticAnalyzer;
import com.taskdsl.semantic.SemanticError;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Ponto de entrada do compilador/interpretador da DSL TaskLang.
 * Uso: java -jar taskdsl-compiler.jar caminho/para/arquivo.tsk
 */
public class Main {

    public static void main(String[] args) {
        // Garante saída em UTF-8 independentemente da configuração de locale do
        // sistema operacional (evita acentos quebrados em Windows/Linux com locale POSIX).
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        if (args.length != 1) {
            System.err.println("Uso: java -jar taskdsl-compiler.jar <arquivo.tsk>");
            System.exit(1);
        }

        Path path = Path.of(args[0]);
        String source;
        try {
            source = Files.readString(path);
        } catch (IOException e) {
            System.err.println("Não foi possível ler o arquivo: " + args[0]);
            System.exit(1);
            return;
        }

        // ---------------- Análise Léxica ----------------
        CharStream input = CharStreams.fromString(source);
        TaskLangLexer lexer = new TaskLangLexer(input);
        CompilerErrorListener lexErrors = new CompilerErrorListener("léxico");
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexErrors);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // ---------------- Análise Sintática ----------------
        TaskLangParser parser = new TaskLangParser(tokens);
        CompilerErrorListener synErrors = new CompilerErrorListener("sintático");
        parser.removeErrorListeners();
        parser.addErrorListener(synErrors);

        TaskLangParser.ProgramContext tree = parser.program();

        // Força o consumo de todos os tokens para capturar erros léxicos que
        // só aparecem fora da árvore sintática efetivamente usada.
        if (lexErrors.hasErrors()) {
            System.err.println("=== Erros léxicos encontrados ===");
            lexErrors.printErrors();
            System.exit(2);
        }
        if (synErrors.hasErrors()) {
            System.err.println("=== Erros sintáticos encontrados ===");
            synErrors.printErrors();
            System.exit(2);
        }

        // ---------------- Construção da AST ----------------
        ASTBuilder builder = new ASTBuilder();
        Stmt.Program program = builder.build(tree);

        // ---------------- Análise Semântica ----------------
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        List<SemanticError> semanticErrors = semanticAnalyzer.analyze(program);
        if (!semanticErrors.isEmpty()) {
            System.err.println("=== Erros semânticos encontrados ===");
            for (SemanticError e : semanticErrors) {
                System.err.println(e);
            }
            System.exit(3);
        }

        // ---------------- Execução (backend: interpretador) ----------------
        Interpreter interpreter = new Interpreter();
        try {
            interpreter.execute(program);
        } catch (RuntimeError e) {
            System.err.println("[Erro de execução] linha " + e.line + ": " + e.getMessage());
            System.exit(4);
        }
    }
}

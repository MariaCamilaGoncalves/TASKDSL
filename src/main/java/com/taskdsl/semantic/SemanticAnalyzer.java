package com.taskdsl.semantic;

import com.taskdsl.ast.Expr;
import com.taskdsl.ast.Stmt;
import com.taskdsl.ast.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Percorre a AST verificando:
 *  - uso de variáveis não declaradas;
 *  - redeclaração de variáveis no mesmo escopo;
 *  - compatibilidade de tipos em declarações, atribuições e expressões
 *    (ex.: impede somar string com int);
 *  - tipos exigidos pelos comandos de domínio (task/assign/complete);
 *  - condições de if/while/for devem ser booleanas (resultado de comparação).
 *
 * Os erros são acumulados em uma lista em vez de interromper na primeira
 * ocorrência, para que o usuário veja todos os problemas de uma vez.
 */
public class SemanticAnalyzer {

    private final SymbolTable symbols = new SymbolTable();
    private final List<SemanticError> errors = new ArrayList<>();

    public List<SemanticError> analyze(Stmt.Program program) {
        for (Stmt s : program.statements) {
            checkStmt(s);
        }
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    private void error(int line, String msg) {
        errors.add(new SemanticError(line, msg));
    }

    // ---------------- Statements ----------------

    private void checkStmt(Stmt stmt) {
        if (stmt instanceof Stmt.Block) {
            checkBlock((Stmt.Block) stmt);
        } else if (stmt instanceof Stmt.VarDecl) {
            checkVarDecl((Stmt.VarDecl) stmt);
        } else if (stmt instanceof Stmt.Assignment) {
            checkAssignment((Stmt.Assignment) stmt);
        } else if (stmt instanceof Stmt.IfStmt) {
            checkIf((Stmt.IfStmt) stmt);
        } else if (stmt instanceof Stmt.WhileStmt) {
            checkWhile((Stmt.WhileStmt) stmt);
        } else if (stmt instanceof Stmt.ForStmt) {
            checkFor((Stmt.ForStmt) stmt);
        } else if (stmt instanceof Stmt.PrintStmt) {
            typeOf(((Stmt.PrintStmt) stmt).expr);
        } else if (stmt instanceof Stmt.TaskDecl) {
            checkTaskDecl((Stmt.TaskDecl) stmt);
        } else if (stmt instanceof Stmt.AssignTaskStmt) {
            checkAssignTask((Stmt.AssignTaskStmt) stmt);
        } else if (stmt instanceof Stmt.CompleteStmt) {
            checkComplete((Stmt.CompleteStmt) stmt);
        } else if (stmt instanceof Stmt.ListTasksStmt) {
            // nada a checar
        } else {
            throw new IllegalStateException("Statement não tratado: " + stmt.getClass());
        }
    }

    private void checkBlock(Stmt.Block block) {
        symbols.enterScope();
        for (Stmt s : block.statements) {
            checkStmt(s);
        }
        symbols.exitScope();
    }

    private void checkVarDecl(Stmt.VarDecl decl) {
        if (!symbols.declare(new Symbol(decl.name, decl.declaredType, decl.line))) {
            Symbol previous = symbols.resolve(decl.name);
            error(decl.line, "variável '" + decl.name + "' já declarada neste escopo " +
                    "(declaração anterior na linha " + previous.declaredAtLine + ")");
        }
        if (decl.init != null) {
            Type initType = typeOf(decl.init);
            if (!assignable(decl.declaredType, initType)) {
                error(decl.line, "não é possível inicializar variável '" + decl.name + "' do tipo " +
                        decl.declaredType.toDisplay() + " com valor do tipo " + initType.toDisplay());
            }
        }
    }

    private void checkAssignment(Stmt.Assignment assign) {
        Symbol symbol = symbols.resolve(assign.name);
        if (symbol == null) {
            error(assign.line, "variável '" + assign.name + "' não foi declarada");
            typeOf(assign.value); // ainda avalia para reportar outros possíveis erros na expressão
            return;
        }
        Type valueType = typeOf(assign.value);
        if (!assignable(symbol.type, valueType)) {
            error(assign.line, "não é possível atribuir valor do tipo " + valueType.toDisplay() +
                    " à variável '" + assign.name + "' do tipo " + symbol.type.toDisplay());
        }
    }

    private void checkIf(Stmt.IfStmt ifStmt) {
        requireBoolean(ifStmt.condition, "condição do 'if'");
        checkBlock(ifStmt.thenBlock);
        if (ifStmt.elseBlock != null) {
            checkBlock(ifStmt.elseBlock);
        }
    }

    private void checkWhile(Stmt.WhileStmt whileStmt) {
        requireBoolean(whileStmt.condition, "condição do 'while'");
        checkBlock(whileStmt.body);
    }

    private void checkFor(Stmt.ForStmt forStmt) {
        symbols.enterScope();
        if (forStmt.initDecl != null) {
            checkVarDecl(forStmt.initDecl);
        } else {
            checkAssignment(forStmt.initAssign);
        }
        requireBoolean(forStmt.condition, "condição do 'for'");
        checkAssignment(forStmt.update);
        // o corpo do for é um bloco próprio, que abre seu próprio sub-escopo
        checkBlock(forStmt.body);
        symbols.exitScope();
    }

    private void requireBoolean(Expr condition, String contexto) {
        Type t = typeOf(condition);
        if (t != Type.BOOL && t != Type.UNKNOWN) {
            error(condition.line, contexto + " deve ser uma expressão booleana (comparação), mas é do tipo " + t.toDisplay());
        }
    }

    // ---------------- Comandos de domínio ----------------

    private void checkTaskDecl(Stmt.TaskDecl decl) {
        Type nameType = typeOf(decl.nameExpr);
        Type prioType = typeOf(decl.priorityExpr);
        if (nameType != Type.STRING && nameType != Type.UNKNOWN) {
            error(decl.line, "o nome da tarefa em 'task' deve ser do tipo string, mas é do tipo " + nameType.toDisplay());
        }
        if (prioType != Type.INT && prioType != Type.UNKNOWN) {
            error(decl.line, "a prioridade em 'task ... priority' deve ser do tipo int, mas é do tipo " + prioType.toDisplay());
        }
    }

    private void checkAssignTask(Stmt.AssignTaskStmt stmt) {
        Type taskType = typeOf(stmt.taskExpr);
        Type personType = typeOf(stmt.personExpr);
        if (taskType != Type.STRING && taskType != Type.UNKNOWN) {
            error(stmt.line, "o nome da tarefa em 'assign' deve ser do tipo string, mas é do tipo " + taskType.toDisplay());
        }
        if (personType != Type.STRING && personType != Type.UNKNOWN) {
            error(stmt.line, "o responsável em 'assign ... to' deve ser do tipo string, mas é do tipo " + personType.toDisplay());
        }
    }

    private void checkComplete(Stmt.CompleteStmt stmt) {
        Type taskType = typeOf(stmt.taskExpr);
        if (taskType != Type.STRING && taskType != Type.UNKNOWN) {
            error(stmt.line, "o nome da tarefa em 'complete' deve ser do tipo string, mas é do tipo " + taskType.toDisplay());
        }
    }

    // ---------------- Expressões ----------------

    /** Calcula (e memoriza em expr.resolvedType) o tipo da expressão, reportando erros de tipo. */
    private Type typeOf(Expr expr) {
        Type result;
        if (expr instanceof Expr.IntLiteral) {
            result = Type.INT;
        } else if (expr instanceof Expr.FloatLiteral) {
            result = Type.FLOAT;
        } else if (expr instanceof Expr.StringLiteral) {
            result = Type.STRING;
        } else if (expr instanceof Expr.BoolLiteral) {
            result = Type.BOOL;
        } else if (expr instanceof Expr.VarRef) {
            Expr.VarRef ref = (Expr.VarRef) expr;
            Symbol symbol = symbols.resolve(ref.name);
            if (symbol == null) {
                error(ref.line, "variável '" + ref.name + "' não foi declarada");
                result = Type.UNKNOWN;
            } else {
                result = symbol.type;
            }
        } else if (expr instanceof Expr.Unary) {
            result = typeOfUnary((Expr.Unary) expr);
        } else if (expr instanceof Expr.Binary) {
            result = typeOfBinary((Expr.Binary) expr);
        } else {
            throw new IllegalStateException("Expressão não tratada: " + expr.getClass());
        }
        expr.resolvedType = result;
        return result;
    }

    private Type typeOfUnary(Expr.Unary unary) {
        Type operand = typeOf(unary.operand);
        if (operand == Type.UNKNOWN) return Type.UNKNOWN;
        if (unary.op.equals("-")) {
            if (!operand.isNumeric()) {
                error(unary.line, "operador unário '-' exige operando numérico (int ou float), mas recebeu " + operand.toDisplay());
                return Type.UNKNOWN;
            }
            return operand;
        } else { // '!'
            if (operand != Type.BOOL) {
                error(unary.line, "operador unário '!' exige operando booleano, mas recebeu " + operand.toDisplay());
                return Type.UNKNOWN;
            }
            return Type.BOOL;
        }
    }

    private Type typeOfBinary(Expr.Binary bin) {
        Type left = typeOf(bin.left);
        Type right = typeOf(bin.right);
        if (left == Type.UNKNOWN || right == Type.UNKNOWN) return Type.UNKNOWN;

        switch (bin.op) {
            case "+":
                if (left == Type.STRING && right == Type.STRING) return Type.STRING;
                if (left.isNumeric() && right.isNumeric()) {
                    return (left == Type.FLOAT || right == Type.FLOAT) ? Type.FLOAT : Type.INT;
                }
                error(bin.line, "operação '+' não é suportada entre " + left.toDisplay() + " e " + right.toDisplay() +
                        " (só é permitida entre dois números ou entre duas strings)");
                return Type.UNKNOWN;

            case "-":
            case "*":
            case "/":
            case "%":
                if (left.isNumeric() && right.isNumeric()) {
                    return (left == Type.FLOAT || right == Type.FLOAT) ? Type.FLOAT : Type.INT;
                }
                error(bin.line, "operação '" + bin.op + "' não é suportada entre " + left.toDisplay() +
                        " e " + right.toDisplay() + " (ambos os operandos devem ser numéricos)");
                return Type.UNKNOWN;

            case "<":
            case ">":
            case "<=":
            case ">=":
                if (left.isNumeric() && right.isNumeric()) return Type.BOOL;
                error(bin.line, "operador '" + bin.op + "' exige operandos numéricos, mas recebeu " +
                        left.toDisplay() + " e " + right.toDisplay());
                return Type.UNKNOWN;

            case "==":
            case "!=":
                if (left == right || (left.isNumeric() && right.isNumeric())) return Type.BOOL;
                error(bin.line, "não é possível comparar " + left.toDisplay() + " com " + right.toDisplay() +
                        " usando '" + bin.op + "'");
                return Type.UNKNOWN;

            case "&&":
            case "||":
                if (left == Type.BOOL && right == Type.BOOL) return Type.BOOL;
                error(bin.line, "operador '" + bin.op + "' exige operandos booleanos, mas recebeu " +
                        left.toDisplay() + " e " + right.toDisplay());
                return Type.UNKNOWN;

            default:
                throw new IllegalStateException("Operador desconhecido: " + bin.op);
        }
    }

    /** Regras de atribuição: int aceita só int; float aceita int/float; string aceita só string. */
    private boolean assignable(Type target, Type source) {
        if (source == Type.UNKNOWN) return true; // erro já reportado antes, evita cascata
        if (target == source) return true;
        if (target == Type.FLOAT && source == Type.INT) return true; // widening implícito
        return false;
    }
}

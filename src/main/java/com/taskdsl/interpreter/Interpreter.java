package com.taskdsl.interpreter;

import com.taskdsl.ast.Expr;
import com.taskdsl.ast.Stmt;
import com.taskdsl.ast.Type;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Backend da DSL: um interpretador tree-walking que executa diretamente a AST
 * (em vez de transpilar para outra linguagem). Assume que a AST já passou
 * pela análise semântica sem erros, então não repete checagens de tipo -
 * apenas converte valores quando necessário (ex.: int -> float).
 */
public class Interpreter {

    private final Environment env = new Environment();
    private final Map<String, Task> tasks = new LinkedHashMap<>();

    public void execute(Stmt.Program program) {
        for (Stmt s : program.statements) {
            exec(s);
        }
    }

    // ---------------- Statements ----------------

    private void exec(Stmt stmt) {
        if (stmt instanceof Stmt.Block) {
            execBlock((Stmt.Block) stmt);
        } else if (stmt instanceof Stmt.VarDecl) {
            execVarDecl((Stmt.VarDecl) stmt);
        } else if (stmt instanceof Stmt.Assignment) {
            execAssignment((Stmt.Assignment) stmt);
        } else if (stmt instanceof Stmt.IfStmt) {
            execIf((Stmt.IfStmt) stmt);
        } else if (stmt instanceof Stmt.WhileStmt) {
            execWhile((Stmt.WhileStmt) stmt);
        } else if (stmt instanceof Stmt.ForStmt) {
            execFor((Stmt.ForStmt) stmt);
        } else if (stmt instanceof Stmt.PrintStmt) {
            System.out.println(format(eval(((Stmt.PrintStmt) stmt).expr)));
        } else if (stmt instanceof Stmt.TaskDecl) {
            execTaskDecl((Stmt.TaskDecl) stmt);
        } else if (stmt instanceof Stmt.AssignTaskStmt) {
            execAssignTask((Stmt.AssignTaskStmt) stmt);
        } else if (stmt instanceof Stmt.CompleteStmt) {
            execComplete((Stmt.CompleteStmt) stmt);
        } else if (stmt instanceof Stmt.ListTasksStmt) {
            execListTasks();
        } else {
            throw new IllegalStateException("Statement não tratado: " + stmt.getClass());
        }
    }

    private void execBlock(Stmt.Block block) {
        env.enterScope();
        for (Stmt s : block.statements) {
            exec(s);
        }
        env.exitScope();
    }

    private void execVarDecl(Stmt.VarDecl decl) {
        Object value;
        if (decl.init != null) {
            value = coerce(decl.declaredType, eval(decl.init));
        } else {
            value = defaultValue(decl.declaredType);
        }
        env.declare(decl.name, value);
    }

    private void execAssignment(Stmt.Assignment assign) {
        Object value = eval(assign.value);
        env.assign(assign.line, assign.name, value);
    }

    private void execIf(Stmt.IfStmt ifStmt) {
        if ((Boolean) eval(ifStmt.condition)) {
            execBlock(ifStmt.thenBlock);
        } else if (ifStmt.elseBlock != null) {
            execBlock(ifStmt.elseBlock);
        }
    }

    private void execWhile(Stmt.WhileStmt whileStmt) {
        while ((Boolean) eval(whileStmt.condition)) {
            execBlock(whileStmt.body);
        }
    }

    private void execFor(Stmt.ForStmt forStmt) {
        env.enterScope();
        if (forStmt.initDecl != null) {
            execVarDecl(forStmt.initDecl);
        } else {
            execAssignment(forStmt.initAssign);
        }
        while ((Boolean) eval(forStmt.condition)) {
            execBlock(forStmt.body);
            execAssignment(forStmt.update);
        }
        env.exitScope();
    }

    // ---------------- Comandos de domínio ----------------

    private void execTaskDecl(Stmt.TaskDecl decl) {
        String name = (String) eval(decl.nameExpr);
        int priority = (Integer) eval(decl.priorityExpr);
        Task task = tasks.get(name);
        if (task == null) {
            task = new Task(name, priority);
            tasks.put(name, task);
            System.out.println("[TaskLang] Tarefa criada: \"" + name + "\" (prioridade " + priority + ")");
        } else {
            task.priority = priority;
            System.out.println("[TaskLang] Prioridade da tarefa \"" + name + "\" atualizada para " + priority);
        }
    }

    private void execAssignTask(Stmt.AssignTaskStmt stmt) {
        String taskName = (String) eval(stmt.taskExpr);
        String person = (String) eval(stmt.personExpr);
        Task task = tasks.get(taskName);
        if (task == null) {
            throw new RuntimeError(stmt.line, "não é possível designar: a tarefa \"" + taskName + "\" não existe (use 'task' para criá-la antes)");
        }
        task.assignee = person;
        System.out.println("[TaskLang] Tarefa \"" + taskName + "\" designada para " + person);
    }

    private void execComplete(Stmt.CompleteStmt stmt) {
        String taskName = (String) eval(stmt.taskExpr);
        Task task = tasks.get(taskName);
        if (task == null) {
            throw new RuntimeError(stmt.line, "não é possível concluir: a tarefa \"" + taskName + "\" não existe");
        }
        task.completed = true;
        System.out.println("[TaskLang] Tarefa \"" + taskName + "\" marcada como concluída");
    }

    private void execListTasks() {
        if (tasks.isEmpty()) {
            System.out.println("[TaskLang] Nenhuma tarefa cadastrada.");
            return;
        }
        System.out.println("[TaskLang] Lista de tarefas:");
        for (Task t : tasks.values()) {
            System.out.println(t);
        }
    }

    // ---------------- Expressões ----------------

    private Object eval(Expr expr) {
        if (expr instanceof Expr.IntLiteral) {
            return ((Expr.IntLiteral) expr).value;
        }
        if (expr instanceof Expr.FloatLiteral) {
            return ((Expr.FloatLiteral) expr).value;
        }
        if (expr instanceof Expr.StringLiteral) {
            return ((Expr.StringLiteral) expr).value;
        }
        if (expr instanceof Expr.BoolLiteral) {
            return ((Expr.BoolLiteral) expr).value;
        }
        if (expr instanceof Expr.VarRef) {
            Expr.VarRef ref = (Expr.VarRef) expr;
            return env.get(ref.line, ref.name);
        }
        if (expr instanceof Expr.Unary) {
            return evalUnary((Expr.Unary) expr);
        }
        if (expr instanceof Expr.Binary) {
            return evalBinary((Expr.Binary) expr);
        }
        throw new IllegalStateException("Expressão não tratada: " + expr.getClass());
    }

    private Object evalUnary(Expr.Unary unary) {
        Object value = eval(unary.operand);
        if (unary.op.equals("-")) {
            if (value instanceof Integer) return -((Integer) value);
            return -((Double) value);
        }
        return !((Boolean) value); // '!'
    }

    private Object evalBinary(Expr.Binary bin) {
        // curto-circuito para && e ||
        if (bin.op.equals("&&")) {
            return (Boolean) eval(bin.left) && (Boolean) eval(bin.right);
        }
        if (bin.op.equals("||")) {
            return (Boolean) eval(bin.left) || (Boolean) eval(bin.right);
        }

        Object left = eval(bin.left);
        Object right = eval(bin.right);

        switch (bin.op) {
            case "+":
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                return arith(bin, left, right, '+');
            case "-":
                return arith(bin, left, right, '-');
            case "*":
                return arith(bin, left, right, '*');
            case "/":
                return arith(bin, left, right, '/');
            case "%":
                return arith(bin, left, right, '%');
            case "<":
                return toDouble(left) < toDouble(right);
            case ">":
                return toDouble(left) > toDouble(right);
            case "<=":
                return toDouble(left) <= toDouble(right);
            case ">=":
                return toDouble(left) >= toDouble(right);
            case "==":
                return valuesEqual(left, right);
            case "!=":
                return !valuesEqual(left, right);
            default:
                throw new IllegalStateException("Operador desconhecido: " + bin.op);
        }
    }

    private Object arith(Expr.Binary bin, Object left, Object right, char op) {
        boolean isFloat = bin.resolvedType == Type.FLOAT;
        if (isFloat) {
            double a = toDouble(left);
            double b = toDouble(right);
            switch (op) {
                case '+': return a + b;
                case '-': return a - b;
                case '*': return a * b;
                case '/':
                    if (b == 0.0) throw new RuntimeError(bin.line, "divisão por zero");
                    return a / b;
                case '%':
                    if (b == 0.0) throw new RuntimeError(bin.line, "divisão por zero (módulo)");
                    return a % b;
            }
        } else {
            int a = (Integer) left;
            int b = (Integer) right;
            switch (op) {
                case '+': return a + b;
                case '-': return a - b;
                case '*': return a * b;
                case '/':
                    if (b == 0) throw new RuntimeError(bin.line, "divisão por zero");
                    return a / b;
                case '%':
                    if (b == 0) throw new RuntimeError(bin.line, "divisão por zero (módulo)");
                    return a % b;
            }
        }
        throw new IllegalStateException("Operador aritmético desconhecido: " + op);
    }

    private boolean valuesEqual(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return toDouble(left) == toDouble(right);
        }
        return left.equals(right);
    }

    private double toDouble(Object value) {
        if (value instanceof Integer) return (Integer) value;
        return (Double) value;
    }

    private Object coerce(Type target, Object value) {
        if (target == Type.FLOAT && value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        return value;
    }

    private Object defaultValue(Type type) {
        switch (type) {
            case INT: return 0;
            case FLOAT: return 0.0;
            case STRING: return "";
            default: return null;
        }
    }

    private String format(Object value) {
        if (value instanceof Double) {
            double d = (Double) value;
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.format("%.1f", d);
            }
            return String.valueOf(d);
        }
        return String.valueOf(value);
    }
}

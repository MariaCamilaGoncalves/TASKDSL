package com.taskdsl.ast;

import java.util.List;

/**
 * Nó base para todos os comandos (statements) da linguagem.
 */
public abstract class Stmt extends Node {
    protected Stmt(int line) {
        super(line);
    }

    public static final class Program extends Node {
        public final List<Stmt> statements;
        public Program(List<Stmt> statements, int line) { super(line); this.statements = statements; }
    }

    public static final class Block extends Stmt {
        public final List<Stmt> statements;
        public Block(List<Stmt> statements, int line) { super(line); this.statements = statements; }
    }

    public static final class VarDecl extends Stmt {
        public final Type declaredType;
        public final String name;
        public final Expr init; // pode ser null
        public VarDecl(Type declaredType, String name, Expr init, int line) {
            super(line);
            this.declaredType = declaredType;
            this.name = name;
            this.init = init;
        }
    }

    public static final class Assignment extends Stmt {
        public final String name;
        public final Expr value;
        public Assignment(String name, Expr value, int line) {
            super(line);
            this.name = name;
            this.value = value;
        }
    }

    public static final class IfStmt extends Stmt {
        public final Expr condition;
        public final Block thenBlock;
        public final Block elseBlock; // pode ser null
        public IfStmt(Expr condition, Block thenBlock, Block elseBlock, int line) {
            super(line);
            this.condition = condition;
            this.thenBlock = thenBlock;
            this.elseBlock = elseBlock;
        }
    }

    public static final class WhileStmt extends Stmt {
        public final Expr condition;
        public final Block body;
        public WhileStmt(Expr condition, Block body, int line) {
            super(line);
            this.condition = condition;
            this.body = body;
        }
    }

    public static final class ForStmt extends Stmt {
        public final VarDecl initDecl;       // usado quando o init declara um novo tipo (pode ser null)
        public final Assignment initAssign;  // usado quando o init reaproveita variável existente (pode ser null)
        public final Expr condition;
        public final Assignment update;
        public final Block body;
        public ForStmt(VarDecl initDecl, Assignment initAssign, Expr condition,
                        Assignment update, Block body, int line) {
            super(line);
            this.initDecl = initDecl;
            this.initAssign = initAssign;
            this.condition = condition;
            this.update = update;
            this.body = body;
        }
    }

    public static final class PrintStmt extends Stmt {
        public final Expr expr;
        public PrintStmt(Expr expr, int line) { super(line); this.expr = expr; }
    }

    // ---------------- Comandos exclusivos do domínio (TaskLang) ----------------

    /** task <expr:string> priority <expr:int>; */
    public static final class TaskDecl extends Stmt {
        public final Expr nameExpr;
        public final Expr priorityExpr;
        public TaskDecl(Expr nameExpr, Expr priorityExpr, int line) {
            super(line);
            this.nameExpr = nameExpr;
            this.priorityExpr = priorityExpr;
        }
    }

    /** assign <expr:string> to <expr:string>; */
    public static final class AssignTaskStmt extends Stmt {
        public final Expr taskExpr;
        public final Expr personExpr;
        public AssignTaskStmt(Expr taskExpr, Expr personExpr, int line) {
            super(line);
            this.taskExpr = taskExpr;
            this.personExpr = personExpr;
        }
    }

    /** complete <expr:string>; */
    public static final class CompleteStmt extends Stmt {
        public final Expr taskExpr;
        public CompleteStmt(Expr taskExpr, int line) { super(line); this.taskExpr = taskExpr; }
    }

    /** list tasks; */
    public static final class ListTasksStmt extends Stmt {
        public ListTasksStmt(int line) { super(line); }
    }
}

package com.taskdsl;

import com.taskdsl.ast.Expr;
import com.taskdsl.ast.Node;
import com.taskdsl.ast.Stmt;
import com.taskdsl.ast.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Converte a árvore de parse gerada pelo ANTLR (TaskLangParser.*Context)
 * na Árvore Sintática Abstrata (AST) definida no pacote com.taskdsl.ast.
 * Essa etapa corresponde ao requisito "gerar a AST" da Análise Sintática.
 */
public class ASTBuilder extends TaskLangBaseVisitor<Node> {

    public Stmt.Program build(TaskLangParser.ProgramContext ctx) {
        List<Stmt> statements = new ArrayList<>();
        for (TaskLangParser.StatementContext s : ctx.statement()) {
            statements.add((Stmt) visit(s));
        }
        return new Stmt.Program(statements, ctx.getStart().getLine());
    }

    // ---------------- Statements ----------------

    @Override
    public Node visitStatement(TaskLangParser.StatementContext ctx) {
        if (ctx.varDecl() != null) return visit(ctx.varDecl());
        if (ctx.assignment() != null) return visit(ctx.assignment());
        if (ctx.ifStmt() != null) return visit(ctx.ifStmt());
        if (ctx.whileStmt() != null) return visit(ctx.whileStmt());
        if (ctx.forStmt() != null) return visit(ctx.forStmt());
        if (ctx.printStmt() != null) return visit(ctx.printStmt());
        if (ctx.taskDecl() != null) return visit(ctx.taskDecl());
        if (ctx.assignTaskStmt() != null) return visit(ctx.assignTaskStmt());
        if (ctx.completeStmt() != null) return visit(ctx.completeStmt());
        if (ctx.listTasksStmt() != null) return visit(ctx.listTasksStmt());
        if (ctx.block() != null) return visit(ctx.block());
        throw new IllegalStateException("Comando não reconhecido na linha " + ctx.getStart().getLine());
    }

    @Override
    public Node visitBlock(TaskLangParser.BlockContext ctx) {
        List<Stmt> statements = new ArrayList<>();
        for (TaskLangParser.StatementContext s : ctx.statement()) {
            statements.add((Stmt) visit(s));
        }
        return new Stmt.Block(statements, ctx.getStart().getLine());
    }

    @Override
    public Node visitVarDecl(TaskLangParser.VarDeclContext ctx) {
        Type type = parseType(ctx.type());
        String name = ctx.ID().getText();
        Expr init = ctx.expr() != null ? (Expr) visit(ctx.expr()) : null;
        return new Stmt.VarDecl(type, name, init, ctx.getStart().getLine());
    }

    @Override
    public Node visitAssignment(TaskLangParser.AssignmentContext ctx) {
        return new Stmt.Assignment(ctx.ID().getText(), (Expr) visit(ctx.expr()), ctx.getStart().getLine());
    }

    @Override
    public Node visitIfStmt(TaskLangParser.IfStmtContext ctx) {
        Expr cond = (Expr) visit(ctx.expr());
        Stmt.Block thenBlock = (Stmt.Block) visit(ctx.block(0));
        Stmt.Block elseBlock = ctx.block(1) != null ? (Stmt.Block) visit(ctx.block(1)) : null;
        return new Stmt.IfStmt(cond, thenBlock, elseBlock, ctx.getStart().getLine());
    }

    @Override
    public Node visitWhileStmt(TaskLangParser.WhileStmtContext ctx) {
        Expr cond = (Expr) visit(ctx.expr());
        Stmt.Block body = (Stmt.Block) visit(ctx.block());
        return new Stmt.WhileStmt(cond, body, ctx.getStart().getLine());
    }

    @Override
    public Node visitForStmt(TaskLangParser.ForStmtContext ctx) {
        TaskLangParser.ForInitContext initCtx = ctx.forInit();
        Stmt.VarDecl initDecl = null;
        Stmt.Assignment initAssign = null;
        if (initCtx.type() != null) {
            Type type = parseType(initCtx.type());
            initDecl = new Stmt.VarDecl(type, initCtx.ID().getText(),
                    (Expr) visit(initCtx.expr()), initCtx.getStart().getLine());
        } else {
            initAssign = new Stmt.Assignment(initCtx.ID().getText(),
                    (Expr) visit(initCtx.expr()), initCtx.getStart().getLine());
        }
        Expr condition = (Expr) visit(ctx.expr(0));
        Stmt.Assignment update = new Stmt.Assignment(ctx.ID().getText(),
                (Expr) visit(ctx.expr(1)), ctx.getStart().getLine());
        Stmt.Block body = (Stmt.Block) visit(ctx.block());
        return new Stmt.ForStmt(initDecl, initAssign, condition, update, body, ctx.getStart().getLine());
    }

    @Override
    public Node visitPrintStmt(TaskLangParser.PrintStmtContext ctx) {
        return new Stmt.PrintStmt((Expr) visit(ctx.expr()), ctx.getStart().getLine());
    }

    @Override
    public Node visitTaskDecl(TaskLangParser.TaskDeclContext ctx) {
        Expr nameExpr = (Expr) visit(ctx.expr(0));
        Expr priorityExpr = (Expr) visit(ctx.expr(1));
        return new Stmt.TaskDecl(nameExpr, priorityExpr, ctx.getStart().getLine());
    }

    @Override
    public Node visitAssignTaskStmt(TaskLangParser.AssignTaskStmtContext ctx) {
        Expr taskExpr = (Expr) visit(ctx.expr(0));
        Expr personExpr = (Expr) visit(ctx.expr(1));
        return new Stmt.AssignTaskStmt(taskExpr, personExpr, ctx.getStart().getLine());
    }

    @Override
    public Node visitCompleteStmt(TaskLangParser.CompleteStmtContext ctx) {
        return new Stmt.CompleteStmt((Expr) visit(ctx.expr()), ctx.getStart().getLine());
    }

    @Override
    public Node visitListTasksStmt(TaskLangParser.ListTasksStmtContext ctx) {
        return new Stmt.ListTasksStmt(ctx.getStart().getLine());
    }

    // ---------------- Expressões ----------------

    @Override
    public Node visitParenExpr(TaskLangParser.ParenExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Node visitUnaryExpr(TaskLangParser.UnaryExprContext ctx) {
        return new Expr.Unary(ctx.op.getText(), (Expr) visit(ctx.expr()), ctx.getStart().getLine());
    }

    @Override
    public Node visitMulDivExpr(TaskLangParser.MulDivExprContext ctx) {
        return new Expr.Binary(ctx.op.getText(), (Expr) visit(ctx.expr(0)), (Expr) visit(ctx.expr(1)), ctx.getStart().getLine());
    }

    @Override
    public Node visitAddSubExpr(TaskLangParser.AddSubExprContext ctx) {
        return new Expr.Binary(ctx.op.getText(), (Expr) visit(ctx.expr(0)), (Expr) visit(ctx.expr(1)), ctx.getStart().getLine());
    }

    @Override
    public Node visitRelExpr(TaskLangParser.RelExprContext ctx) {
        return new Expr.Binary(ctx.op.getText(), (Expr) visit(ctx.expr(0)), (Expr) visit(ctx.expr(1)), ctx.getStart().getLine());
    }

    @Override
    public Node visitEqExpr(TaskLangParser.EqExprContext ctx) {
        return new Expr.Binary(ctx.op.getText(), (Expr) visit(ctx.expr(0)), (Expr) visit(ctx.expr(1)), ctx.getStart().getLine());
    }

    @Override
    public Node visitAndExpr(TaskLangParser.AndExprContext ctx) {
        return new Expr.Binary("&&", (Expr) visit(ctx.expr(0)), (Expr) visit(ctx.expr(1)), ctx.getStart().getLine());
    }

    @Override
    public Node visitOrExpr(TaskLangParser.OrExprContext ctx) {
        return new Expr.Binary("||", (Expr) visit(ctx.expr(0)), (Expr) visit(ctx.expr(1)), ctx.getStart().getLine());
    }

    @Override
    public Node visitIntLitExpr(TaskLangParser.IntLitExprContext ctx) {
        return new Expr.IntLiteral(Integer.parseInt(ctx.getText()), ctx.getStart().getLine());
    }

    @Override
    public Node visitFloatLitExpr(TaskLangParser.FloatLitExprContext ctx) {
        return new Expr.FloatLiteral(Double.parseDouble(ctx.getText()), ctx.getStart().getLine());
    }

    @Override
    public Node visitStringLitExpr(TaskLangParser.StringLitExprContext ctx) {
        String raw = ctx.getText();
        String content = raw.substring(1, raw.length() - 1);
        return new Expr.StringLiteral(unescape(content), ctx.getStart().getLine());
    }

    @Override
    public Node visitTrueExpr(TaskLangParser.TrueExprContext ctx) {
        return new Expr.BoolLiteral(true, ctx.getStart().getLine());
    }

    @Override
    public Node visitFalseExpr(TaskLangParser.FalseExprContext ctx) {
        return new Expr.BoolLiteral(false, ctx.getStart().getLine());
    }

    @Override
    public Node visitVarRefExpr(TaskLangParser.VarRefExprContext ctx) {
        return new Expr.VarRef(ctx.getText(), ctx.getStart().getLine());
    }

    // ---------------- Auxiliares ----------------

    private Type parseType(TaskLangParser.TypeContext ctx) {
        if (ctx.INT() != null) return Type.INT;
        if (ctx.FLOAT() != null) return Type.FLOAT;
        return Type.STRING;
    }

    private String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

package com.interpreters.jlox.components;

import com.interpreters.jlox.Lox;
import com.interpreters.jlox.ast.Expr;
import com.interpreters.jlox.ast.Stmt;
import com.interpreters.jlox.ast.Token;
import com.interpreters.jlox.components.impl.ClassType;
import com.interpreters.jlox.components.impl.FunctionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static com.interpreters.jlox.ast.TokenType.THIS;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final Interpreter interpreter;
    private final Stack<Map<String, Variable>> scopes = new Stack<>();
    private FunctionType curFunction = FunctionType.NONE;
    private ClassType curClass = ClassType.NONE;

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private static class Variable {
        public final Token name;
        public final VariableType type;
        public VariableState state;

        public Variable(Token name, VariableType type, VariableState state) {
            this.name = name;
            this.type = type;
            this.state = state;
        }

        public boolean checkUsed() {
            return state == VariableState.USED ||
                    type == VariableType.METHOD ||
                    "this".equals(name.lexeme);
        }
    }

    private enum VariableState {
        DECLARED,
        DEFINED,
        USED
    }

    private enum VariableType {
        VALUE,
        FUNCTION,
        CLASS,
        METHOD
    }

    public void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name, VariableType.VALUE);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        Variable var = !scopes.isEmpty() ? scopes.peek().get(expr.name.lexeme) : null;
        if (var != null && var.state == VariableState.DECLARED) {
            Lox.error(expr.name, "Can't read local variable in its own initializer.");
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        resolveFunction(stmt, FunctionType.FUNCTION, VariableType.FUNCTION);
        return null;
    }

    private void resolveFunction(Stmt.Function stmt, FunctionType type, VariableType varType) {
        declare(stmt.name, varType);
        define(stmt.name);
        FunctionType prev = curFunction;
        curFunction = type;
        resolve(stmt.lambda);
        curFunction = prev;
    }

    @Override
    public Void visitLambdaExpr(Expr.Lambda lambda) {
        beginScope();
        for (Token param : lambda.params) {
            declare(param, VariableType.VALUE);
            define(param);
        }
        resolve(lambda.body);
        endScope();
        return null;
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        Map<String, Variable> scope = scopes.pop();
        for (Variable v : scope.values()) {
            if (!v.checkUsed()) {
                Lox.warn(v.name, "Unused variable.");
            }
        }
    }

    private void declare(Token name, VariableType type) {
        if (scopes.isEmpty()) return;
        if (scopes.peek().containsKey(name.lexeme)) {
            Lox.error(name, "Already a variable with this name in this scope");
        }
        scopes.peek().put(name.lexeme, new Variable(name, type, VariableState.DECLARED));
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().get(name.lexeme).state = VariableState.DEFINED;
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                scopes.get(i).get(name.lexeme).state = VariableState.USED;
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (ClassType.CLASS != curClass) {
            Lox.error(expr.keyword, "'this' referenced outside of class method.");
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitTernaryExpr(Expr.Ternary expr) {
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (curFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }
        if (stmt.value != null) {
            if (curFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return a value from a class initializer.");
            }
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        declare(stmt.name, VariableType.CLASS);
        define(stmt.name);
        if (stmt.superclass != null) {
            if (stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
                Lox.error(stmt.superclass.name, "A class can't inherit from itself.");
            }
            resolve(stmt.superclass);
        }
        ClassType prev = curClass;
        curClass = ClassType.CLASS;
        beginScope();
        Token thisTok = new Token(THIS, "this", null, 0);
        scopes.peek().put("this", new Variable(thisTok, VariableType.VALUE, VariableState.DEFINED));

        for (Stmt.Function method : stmt.methods) {
            FunctionType type = method.name.lexeme.equals("init") ? FunctionType.INITIALIZER : FunctionType.METHOD;
            resolveFunction(method, type, VariableType.METHOD);
        }
        endScope();
        curClass = prev;
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }
}

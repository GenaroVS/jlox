package com.interpreters.jlox.ast;

import java.util.List;

public abstract class Expr {

    public interface Visitor<R> {
        R visitBinaryExpr(Binary expr);
        R visitLogicalExpr(Logical expr);
        R visitGroupingExpr(Grouping expr);
        R visitTernaryExpr(Ternary expr);
        R visitUnaryExpr(Unary expr);
        R visitLiteralExpr(Literal expr);
        R visitVariableExpr(Variable expr);
        R visitAssignExpr(Assign expr);
        R visitCallExpr(Call expr);
        R visitGetExpr(Get expr);
        R visitSetExpr(Set expr);
        R visitThisExpr(This expr);
        R visitSuperExpr(Super expr);
        R visitLambdaExpr(Lambda expr);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    public static class Binary extends Expr {

        public final Expr left;
        public final Token operator;
        public final Expr right;

        public Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }

    public static class Logical extends Expr {

        public final Expr left;
        public final Token operator;
        public final Expr right;

        public Logical(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }
    }

    public static class Grouping extends Expr {

        public final Expr expression;

        public Grouping(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }

    public static class Ternary extends Expr {

        public final Expr predicate;
        public final Expr left;
        public final Expr right;

        public Ternary(Expr predicate, Expr left, Expr right) {
            this.predicate = predicate;
            this.left = left;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitTernaryExpr(this);
        }
    }

    public static class Unary extends Expr {

        public final Token operator;
        public final Expr right;

        public Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }

    public static class Literal extends Expr {

        public final Object value;

        public Literal(Object value) {
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    public static class Variable extends Expr {

        public final Token name;

        public Variable(Token name) {
            this.name = name;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }
    }

    public static class Assign extends Expr {

        public final Token name;
        public final Expr value;

        public Assign(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitAssignExpr(this);
        }
    }

    public static class Call extends Expr {

        public final Expr callee;
        public final Token paren;
        public final List<Expr> arguments;

        public Call(Expr callee, Token paren, List<Expr> arguments) {
            this.callee = callee;
            this.paren = paren;
            this.arguments = arguments;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitCallExpr(this);
        }
    }

    public static class Get extends Expr {

        public final Token name;
        public final Expr object;

        public Get(Token name, Expr object) {
            this.name = name;
            this.object = object;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGetExpr(this);
        }
    }

    public static class Set extends Expr {

        public final Expr object;
        public final Token name;
        public final Expr value;

        public Set(Expr object, Token name, Expr value) {
            this.object = object;
            this.name = name;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitSetExpr(this);
        }
    }

    public static class This extends Expr {

        public final Token keyword;

        public This(Token keyword) {
            this.keyword = keyword;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitThisExpr(this);
        }
    }

    public static class Super extends Expr {

        public final Token keyword;
        public final Token method;

        public Super(Token keyword, Token method) {
            this.keyword = keyword;
            this.method = method;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitSuperExpr(this);
        }
    }

    public static class Lambda extends Expr {

        public final List<Token> params;
        public final List<Stmt> body;

        public Lambda(List<Token> params, List<Stmt> body) {
            this.params = params;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLambdaExpr(this);
        }
    }

}

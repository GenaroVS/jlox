package com.interpreters.jlox.components;

import com.interpreters.jlox.Lox;
import com.interpreters.jlox.ast.Expr;
import com.interpreters.jlox.ast.Stmt;
import com.interpreters.jlox.ast.Token;
import com.interpreters.jlox.ast.TokenType;

import java.util.ArrayList;
import java.util.List;

import static com.interpreters.jlox.ast.TokenType.*;

public class Parser {
    private final List<Token> tokens;
    private int cur = 0;
    private boolean allowSingleExpression;
    private boolean foundSingleExpression = false;
    private static class ParseError extends RuntimeException {}

    public Parser(List<Token> tokens, boolean allowSingleExpression) {
        this.tokens = tokens;
        this.allowSingleExpression = allowSingleExpression;
    }

    public List<Stmt> parse() {
        List<Stmt>  statements = new ArrayList<>();
        while (!isAtEnd()) {
            Stmt stmt = declaration();
            if (stmt != null) {
                statements.add(stmt);
            }
            if (allowSingleExpression && foundSingleExpression) {
                return statements;
            }
            allowSingleExpression = false;
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) {
                allowSingleExpression = false;
                return varDeclare();
            }
            return statement();
        } catch (ParseError e) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclare() {
        Token name = checkWithError(IDENTIFIER, "Expect variable name.");
        Expr initialVal = null;
        if (match(EQUAL)) {
            initialVal = expression();
        }
        checkWithError(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initialVal);
    }

    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(IF)) return ifStmt();
        if (match(LEFT_BRACE)) return block();

        return expressionStatement();
    }

    private Stmt block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        checkWithError(RIGHT_BRACE, "Expected '}' after block.");
        return new Stmt.Block(statements);
    }

    private Stmt ifStmt() {
        checkWithError(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr conditional = expression();
        checkWithError(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(conditional, thenBranch, elseBranch);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        if (allowSingleExpression && isAtEnd()) {
            foundSingleExpression = true;
        } else {
            checkWithError(SEMICOLON, "Expect ';' after expression.");
        }
        return new Stmt.Expression(expr);
    }

    private Stmt printStatement() {
        Expr expr = expression();
        checkWithError(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(expr);
    }

    private Expr expression() {
        return comma();
    }

    private Expr comma() {
        Expr expr = assignment();

        while (match(COMMA)) {
            Token operator = previous();
            Expr right = assignment();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr assignment() {
        Expr expr = ternary();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr val = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) val).name;
                return new Expr.Assign(name, val);
            }
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr ternary() {
        Expr expr = or();
        if (match(QUESTION)) {
            Expr left = expression();
            checkWithError(COLON, "Expected ':' after the first ternary expression.");
            return new Expr.Ternary(expr, left, ternary());
        }
        return expr;
    }

    private Expr or() {
        Expr left = and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            left = new Expr.Logical(left, operator, right);
        }
        return left;
    }

    private Expr and() {
        Expr left = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            left = new Expr.Logical(left, operator, right);
        }
        return left;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        if (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            return new Expr.Unary(operator, unary());
        }
        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
        if (match(IDENTIFIER)) return new Expr.Variable(previous());

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            checkWithError(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        if (isAtEnd()) {
            throw error(previous(), "Incomplete expression");
        } else {
            throw error(peek(), "Expected expression.");
        }
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token advance() {
        if (!isAtEnd()) cur++;
        return previous();
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token checkWithError(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token peek() {
        return tokens.get(cur);
    }

    private Token previous() {
        return tokens.get(cur - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}

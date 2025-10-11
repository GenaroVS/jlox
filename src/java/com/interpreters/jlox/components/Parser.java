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
    private static class ParseError extends RuntimeException {}

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() {
        List<Stmt>  statements = new ArrayList<>();
        try {
            while (!isAtEnd()) {
                statements.add(statement());
            }
        } catch (ParseError error) {
            return null;
        }
        return statements;
    }

    private Stmt statement() {
        if (match(PRINT)) return printStatement();

        return expressionStatement();
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        checkWithError(SEMICOLON, "Expect ';' after expression.");
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
        Expr expr = ternary();

        while (match(COMMA)) {
            Token operator = previous();
            Expr right = ternary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr ternary() {
        Expr expr = equality();
        if (match(QUESTION)) {
            Expr left = expression();
            checkWithError(COLON, "Expected ':' after the first ternary expression.");
            return new Expr.Ternary(expr, left, ternary());
        }
        return expr;
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

    private Token advance() {
        if (!isAtEnd()) cur++;
        return previous();
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
}

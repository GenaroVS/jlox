package com.interpreters.jlox.components;

import com.interpreters.jlox.Lox;
import com.interpreters.jlox.ast.Expr;
import com.interpreters.jlox.ast.Stmt;
import com.interpreters.jlox.ast.Token;
import com.interpreters.jlox.ast.TokenType;
import com.interpreters.jlox.components.impl.FunctionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.interpreters.jlox.ast.TokenType.*;

public class Parser {
    private final List<Token> tokens;
    private int cur = 0;
    private boolean allowSingleExpression;
    private boolean foundSingleExpression = false;
    private int loopDepth = 0;
    private TokenType prevLoopType = null;
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
        if (match(BREAK)) return breakStmt();
        if (match(CONTINUE)) return continueStmt();
        if (match(PRINT)) return printStatement();
        if (match(FOR)) return forStmt();
        if (match(IF)) return ifStmt();
        if (match(WHILE)) return whileStmt();
        if (match(LEFT_BRACE)) return block();
        if (check(FUN) && checkNext(IDENTIFIER)) {
            advance();
            return function(FunctionType.FUNCTION);
        }
        if (match(CLASS)) return classDeclaration();
        if (match(RETURN)) return returnStmt();

        return expressionStatement();
    }

    private Stmt breakStmt() {
        if (loopDepth <= 0) {
            error(previous(), "'break' is not in a while or for loop.");
        }
        checkWithError(SEMICOLON, "Expect ';' after 'break'.");
        return new Stmt.Break();
    }

    private Stmt continueStmt() {
        if (loopDepth <= 0) {
            error(previous(), "'continue' is not in a while or for loop.");
        }
        checkWithError(SEMICOLON, "Expect ';' after 'continue'.");
        return new Stmt.Continue(prevLoopType);
    }

    private Stmt block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        checkWithError(RIGHT_BRACE, "Expected '}' after block.");
        return new Stmt.Block(statements);
    }

    private Stmt forStmt() {
        checkWithError(LEFT_PAREN, "Expected '(' after 'for'.");
        prevLoopType = FOR;
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclare();
        } else {
            initializer = expressionStatement();
        }
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        checkWithError(SEMICOLON, "Expect ';' after for loop condition.");
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        checkWithError(RIGHT_PAREN, "Expected ')' after for clauses.");
        try {
            loopDepth += 1;
            Stmt body = statement();
            if (increment != null) {
                body = new Stmt.Block(
                        Arrays.asList(body, new Stmt.Expression(increment))
                );
            }
            if (condition != null) {
                body = new Stmt.While(condition, body);
            } else {
                body = new Stmt.While(new Expr.Literal(true), body);
            }
            if (initializer != null) {
                body = new Stmt.Block(
                        Arrays.asList(initializer, body)
                );
            }
            return body;
        } finally {
            loopDepth -= 1;
        }
    }

    private Stmt whileStmt() {
        checkWithError(LEFT_PAREN, "Expected '(' after 'while'.");
        prevLoopType = WHILE;
        Expr cond = expression();
        checkWithError(RIGHT_PAREN, "Expected ')' after condition.");
        try {
            loopDepth += 1;
            Stmt body = statement();
            return new Stmt.While(cond, body);
        } finally {
            loopDepth -= 1;
        }
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

    private Stmt function(FunctionType type) {
        Token name = checkWithError(IDENTIFIER, String.format("Expect %s name", type.toString()));
        return new Stmt.Function(name, lambda(type));
    }

    private Expr.Lambda lambda(FunctionType type) {
        List<Token> params = new ArrayList<>();
        checkWithError(LEFT_PAREN, funcErrorMessage("Expect '(' after %s name.", type));
        if (!check(RIGHT_PAREN)) {
            do {
                if (params.size() >= 255) {
                    error(peek(), funcErrorMessage("Can't have more than 255 parameters in a %s.", type));
                }
                params.add(checkWithError(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        checkWithError(RIGHT_PAREN, funcErrorMessage("Expect ')' after %s parameters.", type));
        checkWithError(LEFT_BRACE, funcErrorMessage("Expect '{' before function body", type));
        List<Stmt> body = ((Stmt.Block) block()).statements;
        return new Expr.Lambda(params, body);
    }

    private String funcErrorMessage(String msg, FunctionType type) {
        return String.format(msg, type.toString());
    }

    private Stmt returnStmt() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        checkWithError(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt classDeclaration() {
        Token name = checkWithError(IDENTIFIER, "Expect a class name.");

        Expr.Variable superclass = null;
        if (match(LESS)) {
            checkWithError(IDENTIFIER, "Expect superclass name.");
            superclass = new Expr.Variable(previous());
        }

        checkWithError(LEFT_BRACE, "Expect '{' before class body.");
        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add((Stmt.Function) function(FunctionType.METHOD));
        }

        checkWithError(RIGHT_BRACE, "Expect '{' after class body.");
        return new Stmt.Class(name, superclass, methods);
    }

    private Stmt printStatement() {
        Expr expr = expression();
        checkWithError(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(expr);
    }

    private Expr expression() {
        if (match(FUN)) {
            return lambda(FunctionType.FUNCTION);
        }
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
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, val);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get) expr;
                return new Expr.Set(get.object, get.name, val);
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
        while (match(SLASH, STAR)) {
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
        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = arguments(expr);
            } else if (match(DOT)) {
                Token name = checkWithError(IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(name, expr);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr arguments(Expr func) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments to a function.");
                }
                Expr arg = expression();
                // when parsing arguments, handle perceived comma operator
                // as just parsing 2 arguments at a time.
                if (arg instanceof Expr.Binary) {
                    Expr.Binary binaryExpr = (Expr.Binary) arg;
                    if (binaryExpr.operator.type == COMMA) {
                        arguments.add(binaryExpr.left);
                        arguments.add(binaryExpr.right);
                    } else {
                        arguments.add(arg);
                    }
                } else {
                    arguments.add(arg);
                }
            } while (match(COMMA));
        }
        Token paren = checkWithError(RIGHT_PAREN, "Expect ')' after function arguments");
        return new Expr.Call(func, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
        if (match(IDENTIFIER)) return new Expr.Variable(previous());
        if (match(THIS)) return new Expr.This(previous());
        if (match(SUPER)) {
            Token keyword = previous();
            checkWithError(DOT, "Expect '.' after 'super'.");
            Token method = checkWithError(IDENTIFIER, "Expect superclass method name.");
            return new Expr.Super(keyword, method);
        }

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

    private boolean checkNext(TokenType tokenType) {
        if (isAtEnd()) return false;
        if (tokens.get(cur + 1).type == EOF) return false;
        return tokens.get(cur + 1).type == tokenType;
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

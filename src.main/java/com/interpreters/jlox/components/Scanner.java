package com.interpreters.jlox.components;

import com.interpreters.jlox.Lox;
import com.interpreters.jlox.model.Token;
import com.interpreters.jlox.model.TokenType;

import java.util.ArrayList;
import java.util.List;

import static com.interpreters.jlox.model.TokenType.*;

public class Scanner {

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int cur = 0;
    private int line = 1;

    public Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = cur;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    public void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '/':
                if (!comments()) {
                    addToken(SLASH);
                }
                break;
            case '!':
                addToken(advanceIf('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(advanceIf('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(advanceIf('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(advanceIf('=') ? GREATER_EQUAL : GREATER);
                break;
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;
            case '\n':
                line++;
                break;
            case '"': string(); break;
            default:
                if (isDigit(c)) {
                    number();

                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected Character.");
                }
                break;
        }
    }

    public boolean isAtEnd() {
        return cur >= source.length();
    }

    private char advance() {
        return source.charAt(cur++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, cur);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean advanceIf(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(cur) != expected) return false;

        cur++;
        return true;
    }

    private char peek() {
        return peek(0);
    }

    private char peek(int ahead) {
        if (cur + ahead >= source.length()) return '\0';
        return source.charAt(cur + ahead);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
        }
        advance();

        addToken(STRING, source.substring(start + 1, cur - 1));
    }

    private void number() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peek(1))) {
            advance();
            while (isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, cur)));
    }

    private void identifier() {
        while(isAlphaNumeric(peek())) advance();

        String text = source.substring(start, cur);
        TokenType token = TokenType.getKeyWord(text);
        if (token != null) {
            addToken(token);
        } else {
            addToken(IDENTIFIER);
        }
    }

    private boolean comments() {
        if (advanceIf('/')) {
            while (peek() != '\n' && !isAtEnd()) advance();
            return true;
        } else if (advanceIf('*')) {
            boolean hasAdvanced = false;
            while ((peek() != '*' && !isAtEnd()) && (peek(1) != '/' && !isAtEnd())) {
                if (peek() == '\n') line++;
                advance();
                hasAdvanced = true;
            }
            if (hasAdvanced) {
                advance();
                advance();
            }
            return true;
        }
        return false;
    }
}

package com.interpreters.jlox;

import com.interpreters.jlox.ast.Stmt;
import com.interpreters.jlox.ast.Token;
import com.interpreters.jlox.ast.TokenType;
import com.interpreters.jlox.components.Interpreter;
import com.interpreters.jlox.components.Parser;
import com.interpreters.jlox.components.Scanner;
import com.interpreters.jlox.components.Resolver;
import com.interpreters.jlox.exceptions.RuntimeError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

    static Interpreter interpreter = new Interpreter();
    static boolean hadParserError = false;
    static boolean hadRuntimeError = false;

    public static void main (String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()), false);

        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            hadRuntimeError = false;
            hadParserError = false;
            run(line, true);
        }
    }

    private static void run(String source, boolean allowSingleExpressions) {
        Scanner scanner = new Scanner(source);
        Parser parser = new Parser(scanner.scanTokens(), allowSingleExpressions);
        List<Stmt> statements = parser.parse();
        //System.out.println(new AstPrinter().print(expression));
        if (statements == null) return;
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);
        if (hadParserError) return;
        interpreter.interpret(statements);
    }

    public static void warn(Token token, String message) {
        error(token, message);
        hadParserError = false;
    }

    public static void runtimeError(RuntimeError error) {
        report(error.token.line, "", error.getMessage());
        hadRuntimeError = true;
    }

    public static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    public static void error(int line, String message) {
        report(line, "", message);
    }

    public static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] ERROR" + where + ": " + message);
        hadParserError = true;
    }
}

package com.interpreters.jlox.components.impl;

import com.interpreters.jlox.ast.Stmt;
import com.interpreters.jlox.ast.Token;
import com.interpreters.jlox.components.Environment;
import com.interpreters.jlox.components.Interpreter;
import com.interpreters.jlox.components.LoxCallable;

import java.util.List;

public class LoxFunction implements LoxCallable {

    private final Stmt.Function declaration;

    public LoxFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment funEnv = new Environment(interpreter.globals);
        for (int i = 0; i < declaration.params.size(); i++) {
            Token param = declaration.params.get(i);
            Object arg = arguments.get(i);
            funEnv.define(param.lexeme, arg);
        }
        interpreter.executeBlock(declaration.body, funEnv);
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}

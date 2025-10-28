package com.interpreters.jlox.components.impl;

import com.interpreters.jlox.ast.Expr;
import com.interpreters.jlox.ast.Token;
import com.interpreters.jlox.components.Environment;
import com.interpreters.jlox.components.Interpreter;
import com.interpreters.jlox.components.LoxCallable;

import java.util.List;

public class LoxFunction implements LoxCallable {

    private final String name;
    private final Expr.Lambda declaration;
    private final Environment closure;

    public LoxFunction(String name, Expr.Lambda declaration, Environment closure) {
        this.name = name;
        this.declaration = declaration;
        this.closure = closure;
    }

    public LoxFunction bind(LoxInstance classInstance) {
        Environment env = new Environment(closure);
        env.define("this", classInstance);
        return new LoxFunction(this.name, this.declaration, env);
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment funEnv = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            Token param = declaration.params.get(i);
            Object arg = arguments.get(i);
            funEnv.define(param.lexeme, arg);
        }
        try {
            interpreter.executeBlock(declaration.body, funEnv);
            return null;
        } catch (Interpreter.Return e) {
            return e.value;
        }
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn " + name + ">";
    }
}

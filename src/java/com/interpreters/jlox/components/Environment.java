package com.interpreters.jlox.components;

import com.interpreters.jlox.ast.Token;
import com.interpreters.jlox.exceptions.RuntimeError;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();
    public final Environment enclosing;

    public Environment() {
        this.enclosing = null;
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    public void define(String name, Object val) {
        values.put(name, val);
    }

    public void assign(Token name, Object val) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, val);
            return;
        }
        if (enclosing != null) {
            enclosing.assign(name, val);
            return;
        }
        throw new RuntimeError(name, String.format("Undefined variable '%s'.", name.lexeme));
    }

    public void assignAt(int depth, Token name, Object val) {
        getAncestor(depth).values.put(name.lexeme, val);
    }

    public Object get(Token token) {
        if (values.containsKey(token.lexeme)) {
            return values.get(token.lexeme);
        }
        if (enclosing != null) return enclosing.get(token);
        throw new RuntimeError(token, String.format("Undefined variable '%s'.", token.lexeme));
    }

    public Object getAt(int depth, String name) {
        return getAncestor(depth).values.get(name);
    }

    private Environment getAncestor(int depth) {
        Environment localEnv = this;
        while (depth-- > 0) {
            localEnv = localEnv.enclosing;
        }
        return localEnv;
    }
}

package com.interpreters.jlox.components;

import com.interpreters.jlox.ast.Token;
import com.interpreters.jlox.exceptions.RuntimeError;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();

    public void define(String name, Object val) {
        values.put(name, val);
    }

    public void assign(Token name, Object val) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, val);
            return;
        }
        throw new RuntimeError(name, String.format("Undefined variable '%s'.", name.lexeme));
    }

    public Object get(Token token) {
        if (values.containsKey(token.lexeme)) {
            return values.get(token.lexeme);
        }
        throw new RuntimeError(token, String.format("Undefined variable '%s'.", token.lexeme));
    }
}

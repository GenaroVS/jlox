package com.interpreters.jlox.components.impl;

import com.interpreters.jlox.ast.Token;
import com.interpreters.jlox.exceptions.RuntimeError;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {

    private LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    public LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    public Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    public void set(Token name, Object value) {
        this.fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}

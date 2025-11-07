package com.interpreters.jlox.components.impl;

import com.interpreters.jlox.components.Interpreter;
import com.interpreters.jlox.components.LoxCallable;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {

    public final String name;
    public final LoxClass superClass;
    public final Map<String, LoxFunction> methods;

    public LoxClass(String name, LoxClass superClass, Map<String, LoxFunction> methods) {
        this.name = name;
        this.superClass = superClass;
        this.methods = methods;
    }

    public LoxFunction findMethod(String name) {
        if (methods.get(name) != null) {
            return methods.get(name);
        }
        if (superClass != null) {
            return superClass.findMethod(name);
        }
        return null;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunction initializer = this.findMethod("init");
        if (initializer != null) {
            initializer
                    .bind(instance)
                    .call(interpreter, arguments);
        }
        return instance;
    }

    @Override
    public int arity() {
        LoxFunction initializer = this.findMethod("init");
        return initializer != null ? initializer.arity() : 0;
    }

    @Override
    public String toString() {
        return name;
    }
}

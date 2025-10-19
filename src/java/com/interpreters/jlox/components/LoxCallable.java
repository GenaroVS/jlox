package com.interpreters.jlox.components;

import java.util.List;

public interface LoxCallable {

    Object call(Interpreter interpreter, List<Object> arguments);

    int arity(); // determines how many arguments to expect
}

package com.interpreters.jlox.components;

import com.interpreters.jlox.Lox;
import com.interpreters.jlox.ast.Expr;
import com.interpreters.jlox.ast.Stmt;
import com.interpreters.jlox.ast.Token;
import com.interpreters.jlox.ast.TokenType;
import com.interpreters.jlox.components.impl.LoxClass;
import com.interpreters.jlox.components.impl.LoxFunction;
import com.interpreters.jlox.components.impl.LoxInstance;
import com.interpreters.jlox.exceptions.RuntimeError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.interpreters.jlox.ast.TokenType.*;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    public final Environment globals = new Environment();
    private Environment env = globals;
    public final Map<Expr, Integer> locals = new HashMap<>();
    private static class BreakLoop extends RuntimeException {};
    private static class ContinueLoop extends RuntimeException {
        public TokenType loopType;
        public ContinueLoop(TokenType loopType) {
            super(null, null, false, false);
            this.loopType = loopType;
        }
    };
    public static class Return extends RuntimeException {
        public Object value;
        public Return(Object value) {
            super(null, null, false, false);
            this.value = value;
        }
    }

    public Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis();
            }

            @Override
            public int arity() {
                return 0;
            }
        });

        globals.define("stringify", new LoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return stringify(arguments.get(0));
            }

            @Override
            public int arity() {
                return 1;
            }
        });
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (RuntimeError e) {
            Lox.runtimeError(e);
        }
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(this.env));
        return null;
    }

    public void executeBlock(List<Stmt> statements, Environment env) {
        Environment previous = this.env;
        try {
            this.env = env;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.env = previous;
        }
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (BreakLoop e) {
                break;
            } catch (ContinueLoop e) {
                if (stmt.body instanceof Stmt.Block && e.loopType == FOR) {
                    List<Stmt> body = ((Stmt.Block) stmt.body).statements;
                    // We need to put the increment expression back into a block to resemble the existing scope
                    // otherwise variable resolving won't work in the increment expression
                    Stmt incrementExpr = body.get(body.size() - 1);
                    Stmt.Block incrementBlock = new Stmt.Block(List.of(incrementExpr));
                    execute(incrementBlock);
                }
            }
        }
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null){
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakLoop();
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        throw new ContinueLoop(stmt.loopType);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object val = stmt.initializer != null ? evaluate(stmt.initializer) : null;
        env.define(stmt.name.lexeme, val);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        env.define(stmt.name.lexeme, new LoxFunction(stmt.name.lexeme, stmt.lambda, env));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        throw new Return(stmt.value != null ? evaluate(stmt.value) : null);
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Object superclass = null;
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass);
            if (!(superclass instanceof LoxClass)) {
                throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
            }
        }
        env.define(stmt.name.lexeme, null);
        if (superclass != null) {
            env = new Environment(env);
            env.define("super", superclass);
        }

        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction loxMethod = new LoxFunction(method.name.lexeme, method.lambda, env);
            methods.put(method.name.lexeme, loxMethod);
        }

        LoxClass klass = new LoxClass(stmt.name.lexeme, (LoxClass)superclass, methods);
        if (superclass != null) env = env.enclosing;
        env.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object val = evaluate(stmt.expression);
        System.out.println(stringify(val));
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case COMMA:
                return right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double) right == 0) {
                    throw new RuntimeError(expr.operator, "Division by zero");
                }
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PLUS: {
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return ((String) left) + ((String) right);
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            }
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
            case LESS:
                checkComparable(expr.operator, left, right);
                if (left == null) {
                    return true;
                }
                if (right == null) {
                    return false;
                }
                if (left instanceof Double) {
                    return (double) left < (double) right;
                }
                if (left instanceof String) {
                    return ((String) left).compareTo((String) right) < 0;
                }
            case LESS_EQUAL:
                checkComparable(expr.operator, left, right);
                if (left == null) {
                    return true;
                }
                if (right == null) {
                    return false;
                }
                if (left instanceof Double) {
                    return (double) left <= (double) right;
                }
                if (left instanceof String) {
                    return ((String) left).compareTo((String) right) <= 0;
                }
            case GREATER:
                checkComparable(expr.operator, left, right);
                if (left == null) {
                    return false;
                }
                if (right == null) {
                    return true;
                }
                if (left instanceof Double) {
                    return (double) left > (double) right;
                }
                if (left instanceof String) {
                    return ((String) left).compareTo((String) right) > 0;
                }
            case GREATER_EQUAL:
                checkComparable(expr.operator, left, right);
                if (left == null) {
                    return false;
                }
                if (right == null) {
                    return true;
                }
                if (left instanceof Double) {
                    return (double) left >= (double) right;
                }
                if (left instanceof String) {
                    return ((String) left).compareTo((String) right) >= 0;
                }
        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object leftResult = evaluate(expr.left);
        if (expr.operator.type == OR) {
            if (isTruthy(leftResult)) return leftResult;
        } else {
            if (!isTruthy(leftResult)) return leftResult;
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookupVariable(expr, expr.name);
    }

    private Object lookupVariable(Expr expr, Token name) {
        Integer depth = locals.get(expr);
        if (depth != null) {
            return env.getAt(depth, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object val = evaluate(expr.value);
        Integer depth = locals.get(expr);
        if (depth != null) {
            env.assignAt(depth, expr.name, val);
        } else {
            globals.assign(expr.name, val);
        }
        return val;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object val = evaluate(expr.right);
        if (expr.operator.type == MINUS) {
            checkNumberOperand(expr.operator, val);
            return -((double)val);
        } else if (expr.operator.type == BANG) {
            return !isTruthy(val);
        }
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        List<Object> args = new ArrayList<>();
        for (Expr arg : expr.arguments) {
            args.add(evaluate(arg));
        }
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and class methods");
        }
        LoxCallable function = (LoxCallable) callee;
        if (args.size() != function.arity()) {
            throw new RuntimeError(expr.paren,
                    String.format("Expected %d arguments but got %d.", function.arity(), args.size()));
        }
        return function.call(this, args);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(expr.name);
        }
        throw new RuntimeError(expr.name, "Only class instances have properties.");
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            Object value = evaluate(expr.value);
            ((LoxInstance) object).set(expr.name, value);
            return value;
        }
        throw new RuntimeError(expr.name, "Only class instances have fields.");
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookupVariable(expr, expr.keyword);
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        int distance = locals.get(expr);
        LoxClass superclass = (LoxClass)env.getAt(distance, "super");
        LoxInstance object = (LoxInstance)env.getAt(distance - 1, "this");
        LoxFunction method = superclass.findMethod(expr.method.lexeme);
        if (method == null) {
            throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
        }
        return method.bind(object);
    }

    @Override
    public Object visitLambdaExpr(Expr.Lambda expr) {
        return new LoxFunction("lambda", expr, env);
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        boolean predicateRes = isTruthy(evaluate(expr.predicate));
        if (predicateRes) {
            return evaluate(expr.left);
        } else {
            return evaluate(expr.right);
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    public void resolve(Expr expr, int scopeDepth) {
        locals.put(expr, scopeDepth);
    }

    private boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (boolean) val;
        return true;
    }

    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        } else if (left != null) {
            return left.equals(right);
        }
        return false;
    }

    private void checkComparable(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        if (left instanceof String && right instanceof String) return;
        if ((left instanceof String || left instanceof Double) && right == null) return;
        if ((right instanceof String || right instanceof Double) && left == null) return;
        throw new RuntimeError(operator, "Operands must both be a number or a string.");
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must both be a number.");
    }
}

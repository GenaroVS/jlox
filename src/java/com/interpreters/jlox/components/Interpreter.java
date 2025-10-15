package com.interpreters.jlox.components;

import com.interpreters.jlox.Lox;
import com.interpreters.jlox.ast.Expr;
import com.interpreters.jlox.ast.Stmt;
import com.interpreters.jlox.ast.Token;
import com.interpreters.jlox.exceptions.RuntimeError;

import java.util.List;

import static com.interpreters.jlox.ast.TokenType.*;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private Environment env = new Environment();

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
        executeBlock(stmt.statements, env);
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
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object val = evaluate(stmt.expression);
        System.out.println(stringify(val));
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object val = evaluate(expr.value);
        env.assign(expr.name, val);
        return val;
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
        return env.get(expr.name);
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

    private void executeBlock(List<Stmt> statements, Environment env) {
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

package lox;

import java.util.*;

/**
 * This is the part which actually compute the expressions and return values
 */
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    /**
     * For each varible expression, we store the number scopes between the current and the scope where the variable is
     * defined.
     * <p>
     * Since in java each object has its own identity, even if the expression has the same names, it is wrapped into an object
     * and so no risk of collision
     */
    private final Map<Expr, Integer> locals = new HashMap<>();
    private Environment environment = globals; // Keep a copy of the global scope

    Interpreter() {
        // We define a native function called clock which returns the times
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            }
            case SLASH -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;
            }
            case STAR -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            }
            case PLUS -> {
                // Addition if numbers
                if (left instanceof Double && right instanceof Double)
                    return (double) left + (double) right;

                // Concatenation if strings
                if (left instanceof String && right instanceof String)
                    return (String) left + (String) right;

                // Case a not all are numbers or strings
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            }
            // Added for powers e.g. 3^(3.5)
            case EXPONENT -> {
                checkNumberOperands(expr.operator, left, right);
                return Math.pow((double) left, (double) right);
            }
            // Comparisons
            case GREATER -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            }
            case GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            }
            case LESS -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            }
            case LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            }

            // Equality
            case EQUAL_EQUAL -> {
                return isEqual(left, right);
            }
            case BANG_EQUAL -> {
                return !isEqual(left, right);
            }

        }

        //Unreachable
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        }
        return function.call(this, arguments);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    /**
     * Notice that we do not explicitely return true or false, but since we have a dynamic type language, we will return
     * the side which is truthy
     * i.e. print 10 or 45; will return 10 and not true
     *
     * @param expr
     * @return
     */
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            /**
             * This is to evaluate the not
             */
            case BANG -> {
                return !isTruthy(right);
            }

            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            }

        }
        // Unreachable code
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookupVariable(expr.name, expr);
    }

    // Only local variables are statically analysed
    private Object lookupVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }


    /**
     * This helps us to detect if an operand is a number
     * Else we throw a runtime error
     */
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    // Checks if two operands are numbers
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    // Helpers
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    /**
     * This is a helper to tell if an expression is true or not.
     * nil and false -> false
     * Others (numbers, strings etc.) -> true
     *
     * @param object an expression
     * @return true if the object is true else false
     */
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    /**
     * Implementation of equality
     */
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    /**
     * this is what does the interpretation
     */
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    /**
     * Helper to execute statements
     *
     * @param stmt
     */
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            // We consider numbers which end with .0 as integers
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        // We create a new scope enclosed in the current environment
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    /**
     * Notice that we first save the current environment(scope), then create se the current environment as the inner
     * one created, then we execute in that scope and at the end, we reload the previous scope
     */
    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;

        }
    }


    /**
     * since statements return no value, we use the Void return type
     *
     * @return nothing
     */
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        // When we create a new function we capture the current environment
        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    /**
     * just a thin wrapper around the Java implementation of the If control flow
     *
     * @param stmt
     * @return
     */
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        // System.out.println("Done ...");
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    /**
     * This is a strange implementation. When a return expression is called we throw an exception and then catch it inorder
     * to exit the stack of execution.
     */
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    /**
     * Interestingly, we handle variable declaration and call
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        // No initializer means nil
        Object value = null;
        if (stmt.initializer != null) value = evaluate(stmt.initializer);

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    /**
     * Resolves a variable and stores it as a key to its distance from current environment
     *
     * @param expr
     */
    public void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }
}

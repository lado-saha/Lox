package lox;

import java.util.List;


/* Heart of the function and method creation and execution in the lox language */
public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    // This is the scope of the current function
    private final Environment closure;

    LoxFunction(Stmt.Function declaration, Environment closure) {
        this.closure = closure;
        this.declaration = declaration;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    /**
     * If the user decides to print the function, we print <fn fun_name>
     *
     * @return
     */
    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }

    /**
     * We store the parameters in a new scope and execute the block then in case of a return value, we catch the Return RuntimeException
     * Each call to a function must result in the creation of its private environment. No two calls of even the same function
     * should share the same environment, if not this will jjbreak recursion.
     *
     * @param interpreter
     * @param arguments
     * @return
     */
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        // Here we try to execute the block and if we get a Return Exception, it means we are returning a value
        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }

        return null;
    }
}

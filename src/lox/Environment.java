package lox;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a class to manage states of things e.g. Variables and  their values etc
 */
public class Environment {
    // This is a reference to the outer scope immediately containing this environment
    final Environment enclosing;
    /**
     * Keys are var_names and values are the var_values
     */
    private final Map<String, Object> values = new HashMap<>();

    // For global scopes environment
    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    /**
     * binds a variable to a value using its name.
     * NB: If it already exists, it will just update the value of the variable
     * <p>
     * We But notice that if the user declares another variable with the same name, we will just crush the older.
     * We leave this feature for REPL. But, it's kinda confusing
     */
    void define(String name, Object value) {
        values.put(name, value);
    }

    /**
     * Try to return the value bound to a variable if it exists else throws error
     * NB: We throw runtime and not compile time error because, we are not sure when this will be executed.
     * i.e. The variable may be declared lower but code higher in terms of line number
     */
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        /*
         If we don't find in the inner scope, we can try to find it in the outer scope
         recursively until we reach the global scope and throw an error
        */
        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    /**
     * To change the value of a previously declared variable
     */
    public void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        // Similarly, we find the variable in the outer-scope to change its value
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    public Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    // Helper to get the outermost environment enclosing this current one by a given distance
    private Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }
        return environment;
    }

    // We assign a variable to its original scope
    public void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, values);
    }
}

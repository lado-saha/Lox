package lox;

import java.util.List;

/**
 * This interface is implemented by all things which can be called e.g. functions, class objects etc.
 */
interface LoxCallable {
    /**
     * This returns the number of arguments required
     */
    int arity();

    Object call(Interpreter interpreter, List<Object> arguments);
}

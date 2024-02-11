package lox;

/**
 * Our own implementation of the Runtime Exception in java.
 */
public class RuntimeError extends RuntimeException {
    final Token token;

    public RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}

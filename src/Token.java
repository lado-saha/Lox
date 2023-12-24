/**
 * We need to know the token type, and line number and many more
 */


public class Token {
    TokenType type;
    String lexeme;
    Object literal;
    int line;

    /**
     * A token is an
     * @param type
     * @param lexeme
     * @param literal
     * @param line
     */
    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }


}

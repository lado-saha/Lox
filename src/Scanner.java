import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    /**
     * This is the source code, or the code to scan
     */
    private final String source;
    /**
     * The list of tokens generated from the code after the scan
     */
    private final List<Token> tokens = new ArrayList<>();

    /**
     * A list of language keywords or reserved words
     */
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", TokenType.AND);
        keywords.put("class", TokenType.CLASS);
        keywords.put("else", TokenType.ELSE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("for", TokenType.FOR);
        keywords.put("fun", TokenType.FUN);
        keywords.put("if", TokenType.IF);
        keywords.put("nil", TokenType.NIL);
        keywords.put("or", TokenType.OR);
        keywords.put("print", TokenType.PRINT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("super", TokenType.SUPER);
        keywords.put("this", TokenType.THIS);
        keywords.put("true", TokenType.TRUE);
        keywords.put("var", TokenType.VAR);
        keywords.put("while", TokenType.WHILE);
    }

    private int start = 0;
    private int current = 0;
    private int line = 1;

    public Scanner(String source) {
        this.source = source;
    }

    /**
     * To know that we have scanned the whole source code
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * Consumes and returns the next character of the source code
     */
    private char advance() {
        current++; // Means consuming the current character
        return source.charAt(current - 1);
    }

    /**
     * Checks if the character is a digit
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * This just checks if we have letter including `_`
     */
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    /**
     * This just checks if we have an alphanumeric character
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    /**
     * This looks at the current unconsumed character but does not consume it
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * This looks at the next unconsumed character but does not consume it
     */
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    /**
     * Appends a token to the list of tokens.
     *
     * @param type
     * @param literal
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    /**
     * This scans the code and return the tokens
     */
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        // We finally append the `End Of File`(EOF) token
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    /**
     * The heart of our scanner or lexer
     */
    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(' -> addToken(TokenType.LEFT_PAREN);
            case ')' -> addToken(TokenType.RIGHT_PAREN);
            case '{' -> addToken(TokenType.LEFT_BRACE);
            case '}' -> addToken(TokenType.RIGHT_BRACE);
            case ',' -> addToken(TokenType.COMMA);
            case '.' -> addToken(TokenType.DOT);
            case '-' -> addToken(TokenType.MINUS);
            case '+' -> addToken(TokenType.PLUS);
            case ';' -> addToken(TokenType.SEMICOLON);
            case '*' -> addToken(TokenType.STAR);
            // This is due to the existence of !=, <=, >= etc. The `match` checks the value of the next character is `=`
            case '!' -> addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
            case '=' -> addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
            case '<' -> addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
            case '>' -> addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);

            // The `/` is kind of special since two of them `//` means a comment while one is a division
            case '/' -> {
                if (match('/')) {
                    // Since a comment goes until the end of the line, we just continue to advance
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(TokenType.SLASH);
                }
            }
            case ' ', '\r', '\t' -> {
                //Ignore spaces, paragraphs, tabs etc
            }
            case '\n' -> line++;

            /*
              handling the literals i.e. string literals
             */
            case '"' -> string();
            /*
             * In case we encounter an unknown character
             * we still continue to scan the rest of the code.
             * But the hadError get set, and thus we won't ever execute even if we continue to scan
             */
            default -> {
                // We check to see if the character is a number
                if (isDigit(c))
                    number();
                    // We suppose our character is the beginning of an identifier e.g. variable name etc
                else if (isAlpha(c)) {
                    identifier();
                } else
                    Lox.error(line, "Unexpected character.");
            }
        }
    }

    /**
     * This is used to save a word token as an identifier(name of varible etc) or reserved word.
     */
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);

        if (type == null) type = TokenType.IDENTIFIER; // Case it is an identifier
        addToken(type);
    }

    /**
     * This is used to scan or lex out a string value enclosed by double-quotes.
     */
    private void string() {
        // Until we reach the closing `"` or the end of the source code
        while (peek() != '"' && !isAtEnd()) {
            // multi-line Strings can cover many lines, with paragraphs etc
            if (peek() == '\n') line++;
            advance();
        }

        // When we comme out of the loop,because we reached the end of the file, we signal an Unterminated string
        if (isAtEnd()) {
            Lox.error(line, "Unterminated string");
            return;
        }
        advance();

        // Extract the string value. If we supported escape sequences like `\n', we could unescape those here
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    /**
     * This is used to consume the numbers
     */
    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part to continue peeping and consuming if the following character after the dot is a number
        if (peek() == '.' && isDigit(peekNext()))
            advance();

        // After the point and in case we have a number, we continue consuming the fractional part
        while (isDigit(peek())) advance();

        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }


    /**
     * This checks if the current character equals the expected character and in that case, we consume the current character
     *
     * @param expected character at the currrent position
     * @return true in the case it matches
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        // We consume the next character also
        current++;
        return true;
    }
}

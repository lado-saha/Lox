package lox;

import java.util.List;

/**
 * Parses the tokens generated from the scanner
 */
public class Parser {
    private final List<Token> tokens;
    // Index of the current to-be consumed token
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * @param type
     * @return true if the current token is of same type as one provided as argument
     */
    private boolean check(TokenType type) {
        if (!isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean isAtEnd() {
        return current >= tokens.size();
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * Tries to match the current token type with the ones provided as argument
     * then consumes it if true
     *
     * @param types
     * @return
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }


    /**
     * An expression is simply an equality for now
     *
     * @return
     */
    private Expr expression() {
        return equality();
    }

    /**
     * equality -> comparison ((`!=` | `==`) comparison)* ;
     *
     * @return a binary expression
     */
    private Expr equality() {
        Expr expr = comparison();

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * comparison -> term ((`>` | `>=` | `<` | `<=`) term)* ;
     * Comparism of greater etc
     *
     * @return
     */
    private Expr comparison() {
        Expr expr = term();

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * term -> factor ((`-` | `+`) factor)*;
     * For addition and subtraction
     *
     * @return
     */
    private Expr term() {
        Expr expr = factor();

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * factor -> unary ((`\` | `*`) unary)*;
     * For multiplications and divisions
     *
     * @return
     */
    private Expr factor() {
        Expr expr = unary();

        while (match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * unary -> (`-`, `!`) unary | primary;
     * This includes negation and multiplication by -1
     *
     * @return
     */
    private Expr unary() {
        if (match(TokenType.MINUS, TokenType.BANG)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    /**
     * primary -> NUMBER | STRING | `true` | `false` | `nil` | `(` expression `)` ;
     * We can always match a higher level expression
     *
     * @return
     */
    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NIL)) return new Expr.Literal(null);
        if (match(TokenType.NUMBER, TokenType.STRING)) return new Expr.Literal(previous().literal);
        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

    }
}

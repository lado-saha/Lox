package lox;

import java.util.ArrayList;
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
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /**
     * We end when we reach the end of the file EOF
     */
    private boolean isAtEnd() {
//        return current >= tokens.size();
        return peek().type == TokenType.EOF;
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
     * Checks to see if the next token is of the expected type in which case it consumes else, we throw an error
     *
     * @return
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) {
//            System.out.println("Consumed " + type.name());
            return advance();
        }
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    /**
     * Manages the runtime errors produced during the parsing
     */
    private static class ParseError extends RuntimeException {

    }

    /**
     * Begins the parsing of our code
     * We parse our code into a list of statements, and we convert it into a list of statements
     * expressions
     */
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
//            System.out.println(statements.getLast());
        }

        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(TokenType.EQUAL)) initializer = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");

        return new Stmt.Var(name, initializer);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        // Continue looping till we find the '{' or end of the code, to avoid infinite loops
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect'}' after block.");
        return statements;
    }

    /**
     * Remember that we have 2 types of expressions
     * print and other expressions
     */
    private Stmt statement() {
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    /**
     * Print expression; We print an expression
     */
    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    /**
     * a statement is something of the sort 'expression ;'
     */
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    /**
     * A strange philosophy which consist of ignoring further tokens after we reach an error until we reach another token
     * worth rechecking
     */
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;

            switch (peek().type) {
                // We skip anything
                case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT -> {
                }
                case RETURN -> {
                    return;
                }
            }

            advance();
        }
    }


    /**
     * An expression is simply an equality for now
     *
     * @return
     */
    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = equality();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
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

        while (match(TokenType.SLASH, TokenType.STAR, TokenType.EXPONENT)) {
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
     */
    private Expr primary() {
//        System.out.println("Primary: " + " " + peek());
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.NIL)) return new Expr.Literal(null);
        if (match(TokenType.NUMBER, TokenType.STRING)) return new Expr.Literal(previous().literal);
        // For var var_name;
        if (match(TokenType.IDENTIFIER)) return new Expr.Variable(previous());
        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
//        if (match(TokenType.EOF)) return new Expr.Literal("goood");

        // Incase we donot find any expression
        throw error(peek(), " Expect Expression");
    }
}

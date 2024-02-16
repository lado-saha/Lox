package lox;

import java.util.ArrayList;
import java.util.Arrays;
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
            if (match(TokenType.FUN)) return function("function");
            if (match(TokenType.VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    /**
     * @param kind either "function" or "method", goals to differentiate functions and class methods
     */
    private Stmt.Function function(String kind) {
        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after" + kind + " name.");
        List<Token> parameters = new ArrayList<>();

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(
                        consume(TokenType.IDENTIFIER, "Expect parameter name.")
                );
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");

        consume(TokenType.LEFT_BRACE, "Expect '{' before " + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
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
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }


    /**
     * Handles return; or return value;
     *
     * @return
     */
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;

        /*
         we check if the return expression returns a value by verifying that after the return we do not immediately get
         a semicolon
        */
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' after the return keyword.");
        return new Stmt.Return(keyword, value);
    }


    /**
     * The for loop is like in C
     * initializer, condition, increment
     *
     * @return
     */
    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

        // Initializer clause
        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        // Condition clause
        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

        // Incrementation clause
        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");
        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
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
     * @return
     */
    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
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
        Expr expr = or();

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

    private Expr or() {
        Expr expr = and();

        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
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

        return call();
    }


    /**
     * This is for handling function calls. e.g. bar(arg1, arg2, ...)()(). We consider bar to be a primary expression.
     * It can be an identifier.
     *
     * @return
     */
    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }

        return expr;
    }

    /**
     * If the next token is ')', it means we have no arguments else we parse them by matching the comma.
     * When we reach the last argument, we make sure the ')' was the last symbol.
     * If we have than 255 arguments, we just warn and continue
     *
     * @return
     */
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) error(peek(), "Can't have more than 255 arguments.");
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
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

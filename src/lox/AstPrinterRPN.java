package lox;

import java.util.Arrays;

/**
 * The visitor's pattern was implemented to replace the inavailability of the Sealed class
 */
public class AstPrinterRPN implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return null;
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return "(" + expr.left + " " + expr.right + " " + expr.operator.lexeme + ")";
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return "(" + expr.expression + " group" + ")";
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        else return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return null;
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return "(" + expr.right + " " + expr.operator.lexeme + ")";
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return null;
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

    for (int i = exprs.length - 1; i >= 0 ; i--) {


        }

       builder.append("(");
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(name);
        builder.append(")");

        return builder.toString();
    }

    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(new Expr.Literal(45.67))
        );
        System.out.println(new AstPrinterRPN().print(expression));
    }
}

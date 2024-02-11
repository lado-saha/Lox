package lox;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    static boolean hadError = false;
    private static Boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();

    /**
     * Basic error handling
     */
    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ":" + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end ", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    /**
     * Actual code runner
     *
     * @param source
     */
    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

//        System.out.println(tokens);
        Parser parser = new Parser(tokens);

        List<Stmt> statements = parser.parse();

        // Stop in case of any error
        if (hadError) return;

        interpreter.interpret(statements);

        // For now, we just print the tokens
    }

    /**
     * Runs the whole file whose path is given as argument when launching the interpreter
     *
     * @throws IOException
     */
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);

    }

    /**
     * Runs line by line when we launch the interpreter without any arguments
     *
     * @throws IOException
     */
    private static void runPrompt() throws IOException {
        while (true){
            InputStreamReader input = new InputStreamReader(System.in);
            BufferedReader reader = new BufferedReader(input);

            System.out.print("\n> ");

            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    public static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}
package com.jlox.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

  private static boolean debugMode = false;
  static boolean hadError = false;
  static boolean hadRuntimeError = false;

  private static Interpreter interpreter = new Interpreter();

  public static void main(String[] args) throws IOException {

    // set debug mode
    String debugEnv = System.getenv("LOX_DEBUG");
    if (debugEnv != null && debugEnv.equals("1")) {
      debugMode = true;
      System.out.println("Debug mode activated. Expressions in REPL will show the AST");
    }

    System.out.println("☀☀☀ Starting the Lox Interpeter ☀☀☀");
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  /*
   * Run a Lox file from path
   */
  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));
    if (hadError)
      System.exit(65);
    if (hadRuntimeError)
      System.exit(70);
  }

  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    // forever loop
    for (;;) {
      System.out.println(">> ");
      String line = reader.readLine();
      if (line == null)
        break;
      run(line);
      hadError = false; // reset the error flag after every successful line
    }
  }

  private static void run(String loxSource) {
    Scanner scanner = new Scanner(loxSource);
    List<Token> tokens = scanner.scan();

    Parser parser = new Parser(tokens);
    Expr expr = parser.parse();

    if (hadError)
      return;

    if (debugMode)
      System.out.println(String.format("AST: %s", new AstPrinter().print(expr)));

    interpreter.interpret(expr);
  }

  static void error(int line, String msg) {
    report(line, "", msg);
  }

  static void runtimeError(RuntimeError re) {
    System.err.println(String.format("line %d: %s", re.token.line, re.getMessage()));
    hadRuntimeError = true;
  }

  private static void report(int line, String where, String msg) {
    System.err.println("[line " + line + "] Error" + where + ": " + msg);
    hadError = true;
  }

  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }
}

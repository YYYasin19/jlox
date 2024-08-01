package com.jlox.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jlox.lox.TokenType.*;

class Parser {

  public static class ParseError extends RuntimeException {
  }

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  Expr parseExpression() {
    try {
      return expression(); // starting point
    } catch (ParseError error) {
      return null;
    }
  }

  List<Stmt> parseStatements() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements;
  }

  private Expr expression() {
    return assignment();
  }

  private Stmt declaration() {
    try {
      if (matchAndAdvance(CLASS))
        return classDecl();
      if (matchAndAdvance(FUN))
        return function("function");
      if (matchAndAdvance(VAR))
        return varDeclaration();
      return statement(); // parse regular statement and print expression
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt.Class classDecl() {
    Token className = consume(IDENTIFIER, "Expected class name");
    consume(LEFT_BRACE, "Expected left curly brace '{' before class definition");

    List<Stmt.Fun> methods = new ArrayList<>();
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      methods.add(function("method"));
    }

    consume(RIGHT_BRACE, "Expected right curly brace '}' after class definition.");
    return new Stmt.Class(className, methods);
  }

  /*
   * fun myFun(a, b, c) { ... }
   */
  private Stmt.Fun function(String kind) {
    Token name = consume(IDENTIFIER, String.format("Expected %s name after keyword", kind));
    consume(LEFT_PAR, "Expected '(' after function name before parameter list");
    List<Token> params = new ArrayList<>();
    if (!check(RIGHT_PAR)) {
      do {
        if (params.size() >= 255) {
          reportError(peek(), String.format("Function %s tries to define more than 255 parameters.", name.lexeme));
        }

        params.add(
            consume(IDENTIFIER, "Expected paremeter name"));

      } while (matchAndAdvance(COMMA));
    }
    consume(RIGHT_PAR, "Expected ')' after function param definition");

    consume(LEFT_BRACE, "Expected '{' for body of function");
    List<Stmt> body = block(); // this already parses the closing bracket

    return new Stmt.Fun(name, params, body);
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expected variable name");

    Expr init = null;
    if (matchAndAdvance(EQUAL)) {
      init = expression();
    }

    consume(SEMICOLON, "Expected ';' after variable declaration");
    return new Stmt.Var(name, init);
  }

  /*
   * statement → exprStmt | printStmt ;
   * exprStmt → expression ";" ;
   * printStmt → "print" expression ";" ;
   */
  private Stmt statement() {

    if (matchAndAdvance(FOR))
      return forStatement();

    if (matchAndAdvance(IF))
      return ifStatement();

    if (matchAndAdvance(RETURN))
      return returnStmt();

    if (matchAndAdvance(PRINT)) // matches and skips the print statement (i.e. 'print')
      return printStatement();

    if (matchAndAdvance(WHILE))
      return whileStatement();

    if (matchAndAdvance(LEFT_BRACE)) {
      return new Stmt.Block(block());
    }

    return expressionStatement();
  }

  /*
   * forStmt → "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression?
   * ")" statement ;
   */
  private Stmt forStatement() {
    consume(LEFT_PAR, "Expect '(' after 'for'");
    Stmt init;
    if (matchAndAdvance(SEMICOLON)) {
      init = null; // no variable init, e.g. for (; ...) {...}
    } else if (matchAndAdvance(VAR)) {
      init = varDeclaration(); // for (var i = 5;;)
    } else {
      init = expressionStatement(); // for (i = 0)
    }

    Expr cond = null;
    // if the next token is not directly a semi-colon, try to parse an expression
    // here
    if (!check(SEMICOLON)) {
      cond = expression();
    }
    consume(SEMICOLON, "Expected a ';' after for loop condition expression");

    Expr inc = null;
    if (!check(RIGHT_PAR)) {
      inc = expression();
    }
    consume(RIGHT_PAR, "Expected a ')' after increment expression of for loop");

    Stmt body = statement();

    // desugaring: there is no for loop block, we instead create the AST for a while
    // loop
    if (inc != null) {
      // create a new body where at the end, the increment is executed
      body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(inc)));
    }

    // create a while loop where the cond for re-running it is the same as the for
    // loop
    if (cond == null)
      cond = new Expr.Literal(true);

    body = new Stmt.While(cond, body);

    if (init != null) {
      body = new Stmt.Block(Arrays.asList(init, body));
    }

    return body;
  }

  private Stmt.If ifStatement() {
    consume(LEFT_PAR, "Expected '(' after 'if'");
    Expr cond = expression();
    consume(RIGHT_PAR, "Expeceted ')' after condition in if-Statement");

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (matchAndAdvance(ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(cond, thenBranch, elseBranch);
  }

  // parse multiple statements + closing }
  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    // until the end of the block '}' parse new statements | delarations
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expected '}' at the end of a block");
    return statements;
  }

  private Stmt.Print printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expected semicolon after print statement");
    return new Stmt.Print(value);
  }

  private Stmt.Return returnStmt() {
    Token keyword = prevToken();
    Expr value = null;
    if (!check(SEMICOLON)) {
      value = expression();
    }

    consume(SEMICOLON, "Expected ';' at the end of a return statement");
    return new Stmt.Return(keyword, value);
  }

  private Stmt.While whileStatement() {
    consume(LEFT_PAR, "Expected '(' after 'while'");
    Expr cond = expression();
    consume(RIGHT_PAR, "Expected ')' after condition expression of while statement");
    Stmt body = statement();

    return new Stmt.While(cond, body);
  }

  private Stmt.Expression expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expected semicolon after expression statement");
    return new Stmt.Expression(expr);
  }

  /*
   * expression → assignment ;
   * assignment → IDENTIFIER "=" assignment | equality ;
   */
  private Expr assignment() {
    Expr expr = or(); // parse the left-hand side -- without knowing what it is!

    if (matchAndAdvance(EQUAL)) {
      Token eq = prevToken();
      Expr rvalue = assignment();

      // check if l-value was a valid assignment target
      // currently, this is only a single variable target, e.g. a = 5
      if (expr instanceof Expr.Variable) {
        Token variableName = ((Expr.Variable) expr).name;
        return new Expr.Assign(variableName, rvalue);
      } else if (expr instanceof Expr.Get) {
        Expr.Get get = (Expr.Get) expr;
        return new Expr.Set(get, get.name, rvalue);
      }

      reportError(eq, String.format("Invalid assignment target: %s", expr.toString()));
    }

    return expr;
  }

  private Expr or() {
    Expr expr = and();

    while (matchAndAdvance(OR)) {
      Token op = prevToken();
      Expr right = and();
      expr = new Expr.Logical(expr, op, right);
    }

    return expr;
  }

  private Expr and() {
    Expr expr = equality();

    while (matchAndAdvance(AND)) {
      Token op = prevToken();
      Expr right = equality();
      expr = new Expr.Logical(expr, op, right);
    }

    return expr;
  }

  /*
   * Rule:
   * equality → comparison ( ( "!=" | "==" ) comparison )* ;
   */
  private Expr equality() {
    Expr expr = comparison();

    while (matchAndAdvance(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = prevToken();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    // custom logic for the ternary operator
    // TODO: Add a Expr.ConditionalExpr(cond, left, right) type to the syntax tree
    // if (peek().type == QUESTION_MARK) {
    // advance(); // skip the question mark
    // Expr leftExpr = expression();
    // Token colon = consume(COLON, "Expected ':' in Ternary expression");
    // Expr rightExpr = expression();
    // return new Expr.Binary(leftExpr, colon, rightExpr);
    // }

    return expr; // either a comparison (e.g. 1 == 2) or a binary expression of a comparison and
                 // something else
  }

  /*
   * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
   */
  private Expr comparison() {
    Expr expr = term();

    while (matchAndAdvance(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = prevToken();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (matchAndAdvance(MINUS, PLUS)) {
      Token operator = prevToken();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /*
   * factor → unary ( ( "/" | "*" ) unary )* ;
   */
  private Expr factor() {
    Expr expr = unary();

    while (matchAndAdvance(SLASH, STAR)) {
      Token operator = prevToken();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /*
   * unary → ( "!" | "-" ) unary | primary ;
   */
  private Expr unary() {
    if (matchAndAdvance(BANG, MINUS)) {
      Token operator = prevToken(); // either ! or -
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return call();
  }

  private Expr call() {
    Expr expr = primary();

    while (true) {
      if (matchAndAdvance(LEFT_PAR)) {
        expr = finishCall(expr);
      } else if (matchAndAdvance(DOT)) {
        Token name = consume(IDENTIFIER, "Expected property name after '.'");
        expr = new Expr.Get(expr, name); // e.g. myObject.a oder myInstance.methodName
      } else {
        break;
      }
    }

    return expr;
  }

  private Expr finishCall(Expr callee) {
    List<Expr> args = new ArrayList<>();

    if (!check(RIGHT_PAR)) {
      do {
        if (args.size() >= 255) {
          reportError(peek(),
              "Can't have more than 255 arguments in Lox. Have you tried passing another data structure?");
        }
        args.add(expression());
      } while (matchAndAdvance(COMMA));
    }

    Token parenthesis = consume(RIGHT_PAR, "Expected ')' after function call argument list");

    return new Expr.Call(callee, parenthesis, args);
  }

  private Expr primary() {
    if (matchAndAdvance(FALSE))
      return new Expr.Literal(false);
    if (matchAndAdvance(TRUE))
      return new Expr.Literal(true);
    if (matchAndAdvance(NIL))
      return new Expr.Literal(null);

    if (matchAndAdvance(NUMBER, STRING)) {
      return new Expr.Literal(prevToken().literal);
    }

    if (matchAndAdvance(IDENTIFIER)) {
      return new Expr.Variable(prevToken());
    }

    if (matchAndAdvance(LEFT_PAR)) {
      Expr expr = expression();
      consume(RIGHT_PAR, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw reportError(peek(), "Expect expression");
  }

  private boolean matchAndAdvance(TokenType... types) {
    for (TokenType ttype : types) {
      if (check(ttype)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private Token consume(TokenType ttype, String message) {
    if (check(ttype))
      return advance();

    throw reportError(peek(), message);
  }

  private ParseError reportError(Token t, String msg) {
    Lox.error(t, msg);
    return new ParseError();
  }

  /*
   * Goes up to the boundary of the current statement (recognized by the
   * semi-colon and keyword)
   * so the parsing can continue from there
   */
  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (prevToken().type == SEMICOLON)
        return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;

        default:
          // just continue
      }
      advance();
    }

  }

  private boolean check(TokenType ttype) {
    if (isAtEnd())
      return false;
    return peek().type == ttype;
  }

  private Token advance() {
    if (!isAtEnd())
      current++;
    return prevToken();
  }

  private Token prevToken() {
    return tokens.get(current - 1);
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }
}

package com.jlox.lox;

import java.util.ArrayList;
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
      if (matchAndAdvance(VAR))
        return varDeclaration();
      return statement(); // parse regular statement and print expression
    } catch (ParseError error) {
      synchronize();
      return null;
    }
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

    if (matchAndAdvance(IF))
      return ifStatement();

    if (matchAndAdvance(PRINT)) // matches and skips the print statement (i.e. 'print')
      return printStatement();

    if (matchAndAdvance(WHILE))
      return whileStatement();

    if (matchAndAdvance(LEFT_BRACE)) {
      return new Stmt.Block(block());
    }

    return expressionStatement();
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
      }

      error(eq, String.format("Invalid assignment target: %s", expr.toString()));
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

    return primary();
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

    throw error(peek(), "Expect expression");
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

    throw error(peek(), message);
  }

  private ParseError error(Token t, String msg) {
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

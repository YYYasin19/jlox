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
      statements.add(statement());
    }

    return statements;
  }

  private Expr expression() {
    return equality();
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
    if (matchAndAdvance(PRINT)) // matches and skips the print statement (i.e. 'print')
      return printStatement();

    return expressionStatement();
  }

  private Stmt.Print printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expected semicolon after print statement");
    return new Stmt.Print(value);
  }

  private Stmt.Expression expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expected semicolon after expression statement");
    return new Stmt.Expression(expr);
  }

  /*
   * Rule:
   * equality → comparison ( ( "!=" | "==" ) comparison )* ;
   */
  private Expr equality() {
    Expr expr = comparison();

    while (matchAndAdvance(BANG_EQUAL, EQUAL_EQUAL, AND, OR)) {
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

package com.craftinginterpreters.lox;

import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {

  private static class ParseError extends RuntimeException {
  }

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  Expr parse() {
    try {
      return expression(); // starting point
    } catch (ParseError error) {
      return null;
    }
  }

  private Expr expression() {
    return equality();
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

package com.craftinginterpreters.lox;

/*
 * Each token is represented by a string in the source (the lexeme) and where this string was found (e.g. for error handling)
 * as well as the TokenType (e.g. 'LITERAL' or 'WHILE')
 */
class Token {
  final TokenType type;
  final String lexeme;
  final Object literal;
  final int line;

  Token(TokenType type, String lexeme, Object literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}

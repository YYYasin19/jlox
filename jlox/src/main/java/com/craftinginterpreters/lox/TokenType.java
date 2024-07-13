package com.craftinginterpreters.lox;

/*
 * Enum of all allowed types of tokens (i.e. reserved keywords)
 */
enum TokenType {
  LEFT_PAR, RIGHT_PAR, LEFT_BRACE, RIGHT_BRACE, COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

  IDENTIFIER, STRING, NUMBER,

  AND, CLASS, ELSE, TRUE, FALSE, FUN, FOR, IF, NIL, OR, PRINT, RETURN, SUPER, THIS, VAR, WHILE, EOF
}

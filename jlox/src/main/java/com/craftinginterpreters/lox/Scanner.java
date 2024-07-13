package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Given a valid .lox source file, tokenizes the source
 */
public class Scanner {
  private final String source; // immutable source input
  private final List<Token> tokens = new ArrayList<>(); // final list of scanned tokens;

  // bookkeeping for the scanning process
  private int start, current, line = 0;

  Scanner(String source) {
    this.source = source;
  }

  List<Token> scan() {
    while (!isAtEnd()) {
      start = current;
      scanToken();
    }

    // append a final EOF token
    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private void scanToken() {
    char c = nextToken();

    switch (c) {
      case '(':
        addToken(LEFT_PAR);
        break;
      case ')':
        addToken(RIGHT_PAR);
        break;
      case '{':
        addToken(LEFT_BRACE);
        break;
      case '}':
        addToken(RIGHT_BRACE);
        break;
      case ',':
        addToken(COMMA);
        break;
      case '.':
        addToken(DOT);
        break;
      case '-':
        addToken(MINUS);
        break;
      case '+':
        addToken(PLUS);
        break;
      case ';':
        addToken(SEMICOLON);
        break;
      case '*':
        addToken(STAR);
        break;

      // the following cases implement two-char lexemes
      // if the next char is the second-part -> create a new token of that type
      case '!':
        addToken(matchNext('=') ? BANG_EQUAL : BANG);
        break;
      case '=':
        addToken(matchNext('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '<':
        addToken(matchNext('=') ? LESS_EQUAL : LESS);
        break;
      case '>':
        addToken(matchNext('=') ? GREATER_EQUAL : GREATER);
        break;

      // the '/' operator is more complicated since comment lines can begin with that
      // as well
      case '/':
        if (matchNext('/')) {
          // this causes lines starting with '//' to be ignored!
          while (peek() != '\n' && !isAtEnd()) {
            nextToken();
          }
        } else {
          addToken(SLASH);
        }
        break;

      case ' ':
      case '\r':
      case '\t':
        break;

      case '\n':
        line++;
        break;

      default:
        Lox.error(line, String.format("Unexpected character '%s'", c));
        break;
    }

  }

  /*
   * Retrieve next token and advance pointer
   */
  private char nextToken() {
    return source.charAt(current++);
  }

  private void addToken(TokenType ttype) {
    addToken(ttype, null);
  }

  /*
   * Add the current token under the pointer to our token list with the given
   * token type
   */
  private void addToken(TokenType ttype, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(ttype, text, literal, line));
  }

  private boolean matchNext(char expected) {
    if (isAtEnd())
      return false;
    if (source.charAt(current) != expected)
      return false;

    // the next character matched!
    // so we can advance the pointer and return a true
    current++;
    return true;
  }

  /**
   * Lookahead: get the next character (or EOF) without advancing the position
   */
  private char peek() {
    return isAtEnd() ? '\0' : source.charAt(current);
  }

}

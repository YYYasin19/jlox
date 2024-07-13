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
}

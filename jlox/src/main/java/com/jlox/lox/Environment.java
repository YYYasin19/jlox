package com.jlox.lox;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class Environment {
  private final Map<String, Object> values = new HashMap<>();

  String getStringRepr() {
    return values.keySet().stream()
        .map(key -> key + "=" + values.get(key))
        .collect(Collectors.joining(", ", "{", "}"));

  }

  void define(String name, Object value) {
    values.put(name, value);
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }

    throw new RuntimeError(name, String.format("Tried to access undefined variable %s", name.lexeme));
  }

  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    throw new RuntimeError(name, String.format("Cannot assign to variable %s. Variable does not exist", name.lexeme));
  }
}

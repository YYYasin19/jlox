package com.jlox.lox;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class Environment {

  private final Environment parentEnv; // reference to the parent-environment
  private final Map<String, Object> values = new HashMap<>();

  Environment() {
    this.parentEnv = null;
  }

  Environment(Environment parent) {
    this.parentEnv = parent;
  }

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

    if (parentEnv != null)
      return parentEnv.get(name);

    throw new RuntimeError(name, String.format("Tried to access undefined variable %s", name.lexeme));
  }

  Object getAt(Integer dist, String name) {
    return ancestor(dist).values.get(name);
  }

  Environment ancestor(Integer dist) {
    Environment env = this;
    for (int i = 0; i < dist; i++) {
      env = env.parentEnv;
    }

    return env;
  }

  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    if (parentEnv != null) {
      parentEnv.assign(name, value);
      return;
    }

    throw new RuntimeError(name, String.format("Cannot assign to variable %s. Variable does not exist", name.lexeme));
  }

  void assignAt(Integer dist, Token name, Object value) {
    ancestor(dist).values.put(name.lexeme, value);
  }
}

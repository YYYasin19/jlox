package com.jlox.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Fun declaration;

  LoxFunction(Stmt.Fun declaration) {
    this.declaration = declaration;
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public String toString() {
    return String.format("<fn %s>", declaration.name.lexeme);
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> args) {
    Environment env = new Environment(interpreter.globals);

    // add all given arguments to the environment
    for (int argIndex = 0; argIndex < declaration.params.size(); argIndex++) {
      // bind concrete argument for the call to the name of the param at this position
      env.define(declaration.params.get(argIndex).lexeme, args.get(argIndex));
    }

    try {
      interpreter.evaluateBlock(declaration.body, env);
    } catch (Return r) {
      return r.value; // evaluated expression or null
    }
    return null;
  }
}

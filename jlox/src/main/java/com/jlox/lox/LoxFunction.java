package com.jlox.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Environment closure;
  private final Stmt.Fun declaration;

  LoxFunction(Stmt.Fun declaration, Environment closure) {
    this.declaration = declaration;
    this.closure = closure;
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
    Environment env = closure;

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

  LoxFunction bind(LoxInstance instance) {
    // capture the current functions environment
    Environment env = new Environment(closure);
    env.define("this", instance); // add 'this' to the current env
    return new LoxFunction(declaration, env); // return a new function with the updated environment
  }
}

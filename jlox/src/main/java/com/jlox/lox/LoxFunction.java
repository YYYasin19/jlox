package com.jlox.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Environment closure;
  private final Stmt.Fun declaration;
  private final Boolean isInit;

  LoxFunction(Stmt.Fun declaration, Environment closure, Boolean isInit) {
    this.declaration = declaration;
    this.closure = closure;
    this.isInit = isInit;
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
    Environment localFuncEnvironment = new Environment(closure);

    for (int argIndex = 0; argIndex < declaration.params.size(); argIndex++) {
      // bind concrete argument for the call to the name of the param at this position
      localFuncEnvironment.define(declaration.params.get(argIndex).lexeme, args.get(argIndex));
    }

    try {
      interpreter.evaluateBlock(declaration.body, localFuncEnvironment);
    } catch (Return r) {
      // in 'init' an empty return will return 'this'
      if (isInit)
        return closure.getAt(0, "this");
      return r.value; // evaluated expression or null
    }

    if (isInit)
      return closure.getAt(0, "this");

    return null;
  }

  LoxFunction bind(LoxInstance instance) {
    // capture the current functions environment
    Environment env = new Environment(closure);
    env.define("this", instance); // add 'this' to the current env
    return new LoxFunction(declaration, env, isInit); // return a new function with the updated environment
  }
}

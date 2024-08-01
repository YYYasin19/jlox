package com.jlox.lox;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

class ClockFn implements LoxCallable {
  @Override
  public int arity() {
    return 0;
  }

  @Override
  public Object call(Interpreter interpreter,
      List<Object> arguments) {
    return (double) System.currentTimeMillis() / 1000.0;
  }

  @Override
  public String toString() {
    return "<native fn>";
  }
}

class Interpreter implements Stmt.Visitor<Void>, Expr.Visitor<Object> {

  boolean debugMode = false;
  final Environment globals = new Environment();
  private Environment env = globals; // env is a pointer to the current env, global always references the global env
  private final Map<Expr, Integer> locals = new HashMap<>(); // for each Syntax Tree node stores the depth

  Interpreter() {
    globals.define("clock", new ClockFn());
  }

  String getEnvStringRepr() {
    return env.getStringRepr();
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt stmt : statements) {
        execStatement(stmt);
      }
    } catch (RuntimeError re) {
      Lox.runtimeError(re);
    }
  }

  private void execStatement(Stmt stmt) {
    stmt.accept(this);
  }

  void resolveToLocals(Expr expr, Integer depth) {
    locals.put(expr, depth);
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.cond))) {
      execStatement(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execStatement(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    env.define(stmt.name.lexeme, null);
    LoxClass cls = new LoxClass(stmt.name.lexeme);
    env.assign(stmt.name, cls);
    return null;
  }

  @Override
  public Void visitFunStmt(Stmt.Fun stmt) {
    // use the current environment as the base for when the function is created
    // that will allow the function to access all variables in there
    // even when the function is returned from another function
    LoxFunction fun = new LoxFunction(stmt, env);
    env.define(stmt.name.lexeme, fun);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) {
      value = evaluate(stmt.value);
    }

    throw new Return(value); // this will be caught by call()
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.cond))) {
      execStatement(stmt.body);
    }

    return null;
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object leftResult = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      // OR
      if (isTruthy(leftResult))
        return leftResult;
    } else { // AND
      if (!isTruthy(leftResult))
        return leftResult;
    }

    return evaluate(expr.right);
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee); // e.g. a literal string ref to the function name or another function call

    List<Object> args = new ArrayList<>();
    for (Expr arg : expr.args) {
      args.add(evaluate(arg));
    }

    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.parenthesis, String.format("Can only call functions and classes, not '%s'", callee));
    }

    LoxCallable fun = (LoxCallable) callee;

    // check argument size
    if (args.size() != fun.arity()) {
      throw new RuntimeError(expr.parenthesis,
          String.format("Wrong number of arguments: %s instead of %s", args.size(), fun.arity()));
    }

    return fun.call(this, args);
  }

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    // Example: myObject.attribute with Expr [object][name]
    Object obj = evaluate(expr.object);
    if (obj instanceof LoxInstance) {
      return ((LoxInstance) obj).get(expr.name);
    }

    throw new RuntimeError(expr.name,
        String.format("Tried to access %s on %s but %s it not an instance object", expr.name, obj, obj));
  }

  @Override
  public Object visitSetExpr(Expr.Set expr) {
    // Example: myInstance.attribute1.field = sum([1,2,3])
    // with [object].[name] = [value]
    Object obj = evaluate(expr.object);
    if (!(obj instanceof LoxInstance)) {
      throw new RuntimeError(expr.name,
          String.format("Cannot set fields on variables (%s) that are not instances", expr.name));
    }

    Object rvalue = evaluate(expr.value);
    ((LoxInstance) obj).set(expr.name, rvalue);
    return rvalue;

  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {

    // eval the block passing the current env down (as a lookup for variables)
    evaluateBlock(stmt.statements, new Environment(env));
    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);

    Integer dist = locals.get(expr);
    if (dist != null) {
      env.assignAt(dist, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }

    return value;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    env.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookupVariable(expr.name, expr);
  }

  private Object lookupVariable(Token name, Expr expr) {
    Integer dist = locals.get(expr);
    if (dist != null) {
      return env.getAt(dist, name.lexeme);
    } else {
      // if we don't find a distance, it must be a global variable
      return globals.get(name);
    }
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null; // we need to fulfill the signature
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    System.out.println(evaluate(stmt.expression));
    return null;
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression); // evaluate sub-expression
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);
    // this is DYNAMIC typing - interpret the data in the 'correct' type in runtime
    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double) right; // assume 'right' is a number, cast it to one and subtract
      default:
        throw new RuntimeException(
            String.format("Unsupported Operator for Unary expression: %s", expr.operator.lexeme));
    }

  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left - (double) right;
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double) left / (double) right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double) left * (double) right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        } else if (left instanceof String && right instanceof String) {
          return (String) left + (String) right;
        } else {
          throw new RuntimeError(expr.operator,
              String.format("Operands for '+' must be two numbers or two strings but were: %s and %s", left, right));
        }
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double) left > (double) right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left >= (double) right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left < (double) right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left <= (double) right;
      case BANG_EQUAL:
        return !isEqual(left, right);
      case EQUAL_EQUAL:
        return isEqual(left, right);
      // case OR:
      // checkBooleanOperands(expr.operator, left, right);
      // return (boolean) left || (boolean) right;
      // case AND:
      // checkBooleanOperands(expr.operator, left, right);
      // return (boolean) left && (boolean) right;
      default:
        throw new RuntimeException(
            String.format("Unsupported Operator for Binary expression: %s", expr.operator.lexeme));
    }

  }

  private Object evaluate(Expr expr) {
    return expr.accept(this); // will call this Visitor again with the appropriate type
  }

  void evaluateBlock(List<Stmt> statements, Environment blockEnv) {
    Environment prevEnv = this.env;

    // execute all statements in this block using the given environment
    try {
      this.env = blockEnv; // this env has a ref to the parent (prev) env
      for (Stmt stmt : statements) {
        execStatement(stmt);
      }
      // then revert back to the original environment
    } finally {
      this.env = prevEnv;
    }
  }

  private boolean isTruthy(Object obj) {
    if (obj == null)
      return false;
    if (obj instanceof Boolean)
      return (boolean) obj;
    return true;
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null)
      return true;
    if (a == null)
      return false;

    return a.equals(b);
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double)
      return;
    throw new RuntimeError(operator, String.format("Operand must be a number but was %s", operand));
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double)
      return;

    throw new RuntimeError(operator, String.format("Operands must be numbers but were %s and %s", left, right));
  }

  private void checkBooleanOperands(Token operator, Object left, Object right) {
    if (left instanceof Boolean && right instanceof Boolean)
      return;

    throw new RuntimeError(operator,
        String.format("Both operands need to be boolean values but were %s and %s", left, right));
  }

  private String stringify(Object object) {
    if (object == null)
      return "nil";

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }
}

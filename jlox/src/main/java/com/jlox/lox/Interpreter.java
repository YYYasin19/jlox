package com.jlox.lox;

import java.util.List;

class Interpreter implements Stmt.Visitor<Void>, Expr.Visitor<Object> {

  private Environment env = new Environment();

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

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object evalValue = evaluate(expr.value);
    env.assign(expr.name, evalValue);
    return evalValue;
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
    return env.get(expr.name);
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
      case OR:
        checkBooleanOperands(expr.operator, left, right);
        return (boolean) left || (boolean) right;
      case AND:
        checkBooleanOperands(expr.operator, left, right);
        return (boolean) left && (boolean) right;
      default:
        throw new RuntimeException(
            String.format("Unsupported Operator for Binary expression: %s", expr.operator.lexeme));
    }

  }

  private Object evaluate(Expr expr) {
    return expr.accept(this); // will call this Visitor again with the appropriate type
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

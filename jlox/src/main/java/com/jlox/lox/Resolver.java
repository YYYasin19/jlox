package com.jlox.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>(); // stack to push and pop scopes
  private FunctionType currentFun = FunctionType.NONE;

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  private enum FunctionType {
    NONE,
    FUNCTION
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }

  private void endScope() {
    scopes.pop();
  }

  void resolve(List<Stmt> stmts) {
    for (Stmt stmt : stmts) {
      resolve(stmt);
    }
  }

  private void resolve(Stmt stmt) {
    // pass it down to other functions of the visitor
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  private void resolveLocal(Expr expr, Token name) {

    // traverse the scope backwards
    for (int i = scopes.size() - 1; i >= 0; i--) {

      // until the variable is found
      if (scopes.get(i).containsKey(name.lexeme)) {

        // resolve that particular scope
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
  }

  private void resolveFunction(Stmt.Fun fun, FunctionType ftype) {

    FunctionType enclosingFun = currentFun;
    currentFun = ftype;

    beginScope();

    for (Token param : fun.params) {
      declare(param);
      define(param);
    }

    resolve(fun.body);
    endScope();

    currentFun = enclosingFun;
  }

  private void declare(Token name) {
    if (scopes.isEmpty())
      return;

    Map<String, Boolean> scope = scopes.peek();
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name, String.format("There is already a variable with the name '%s' in the scope", name.lexeme));
    }
    scope.put(name.lexeme, false); // false == 'not ready yet'
  }

  private void define(Token name) {
    if (scopes.empty())
      return;

    scopes.peek().put(name.lexeme, true);
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if ((!scopes.isEmpty()) && (scopes.peek().get(expr.name.lexeme) == Boolean.FALSE)) {
      Lox.error(expr.name, String.format("Can't read local variable in it's own initializer"));
    }

    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitFunStmt(Stmt.Fun fun) {
    declare(fun.name);
    define(fun.name);

    resolveFunction(fun, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.cond);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null)
      resolve(stmt.elseBranch);

    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {

    if (currentFun == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Can't return from top-level code. You sure you don't want a function here?");
    }
    if (stmt.value != null) {
      resolve(stmt.value);
    }

    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.cond);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);

    for (Expr argument : expr.args) {
      resolve(argument);
    }

    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }
}

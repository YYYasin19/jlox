package com.jlox.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }
    String outputDir = args[0];

    defineAst(outputDir, "Expr", Arrays.asList(
        "Assign: Token name, Expr value",
        "Binary: Expr left, Token operator, Expr right",
        "Call: Expr callee, Token parenthesis, List<Expr> args", // the parenthesis token is stored for debugging info
        "Get: Expr object, Token name",
        "Grouping: Expr expression",
        "Literal: Object value",
        "Logical: Expr left, Token operator, Expr right", // special case of Binary
        "Set: Expr object, Token name, Expr value",
        "Unary: Token operator, Expr right",
        "Variable: Token name"));

    defineAst(outputDir, "Stmt", Arrays.asList(
        "Block: List<Stmt> statements",
        "Class: Token name, List<Stmt.Fun> methods",
        "Expression: Expr expression",
        "Fun: Token name, List<Token> params, List<Stmt> body",
        "If: Expr cond, Stmt thenBranch, Stmt elseBranch",
        "While: Expr cond, Stmt body",
        "Print: Expr expression",
        "Return: Token keyword, Expr value",
        "Var: Token name, Expr initializer"));
  }

  /*
   * Autocreate code for defining classes for different kinds of expressions
   */
  private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
    String path = String.format("%s/%s.java", outputDir, baseName);
    PrintWriter writer = new PrintWriter(path, "utf-8");

    writer.println("package com.jlox.lox;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();
    writer.println("abstract class " + baseName + " {");

    defineVisitor(writer, baseName, types);

    for (String type : types) {
      String className = type.split(":")[0].trim();
      String fields = type.split(":")[1].trim();
      defineType(writer, baseName, className, fields);
    }

    // define the accept method for this type
    writer.println();
    writer.println("  abstract <R> R accept(Visitor<R> visitor);");

    writer.println("}");
    writer.close();

  }

  private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
    writer.println("  static class " + className + " extends " + baseName + " {");

    // Constructor.
    writer.println("    " + className + "(" + fieldList + ") {");

    // Store parameters in fields.
    String[] fields = fieldList.split(", ");
    for (String field : fields) {
      String name = field.split(" ")[1];
      writer.println("      this." + name + " = " + name + ";");
    }

    writer.println("    }");

    // Visitor pattern.
    writer.println();
    writer.println("    @Override");
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    writer.println("      return visitor.visit" +
        className + baseName + "(this);");
    writer.println("    }");

    // Fields.
    writer.println();
    for (String field : fields) {
      writer.println("    final " + field + ";");
    }

    writer.println("  }");
  }

  /*
   * Writes code for the Visitor Interface for each base class
   */
  private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
    writer.println("  interface Visitor<R> {");

    for (String type : types) {
      String typeName = type.split(":")[0].trim();
      // Defines a generic method for each of the possible types: Binary, ...,
      // Example: R visitUnaryExpr(Unary expr);
      writer.println("    R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
    }

    writer.println("  }");
  }
}

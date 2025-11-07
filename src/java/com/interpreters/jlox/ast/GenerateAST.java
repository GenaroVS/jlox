package com.interpreters.jlox.ast;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAST {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
                "Binary   : Expr left, Token operator, Expr right",
                "Logical  : Expr left, Token operator, Expr right",
                "Grouping : Expr expression",
                "Ternary  : Expr predicate, Expr left, Expr right",
                "Unary    : Token operator, Expr right",
                "Literal  : Object value",
                "Variable : Token name",
                "Assign   : Token name, Expr value",
                "Call     : Expr callee, Token paren, List<Expr> arguments",
                "Get      : Token name, Expr object",
                "Set      : Expr object, Token name, Expr value",
                "This     : Token keyword",
                "Super    : Token keyword, Token method",
                "Lambda : List<Token> params, List<Stmt> body"
        ));

        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block      : List<Stmt> statements",
                "While      : Expr condition, Stmt body",
                "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Break      : ",
                "Continue   : TokenType loopType",
                "Var        : Token name, Expr initializer",
                "Expression : Expr expression",
                "Function   : Token name, Expr.Lambda lambda",
                "Return     : Token keyword, Expr value",
                "Class      : Token name, Expr.Variable superclass," +
                        " List<Stmt.Function> methods",
                "Print      : Expr expression"
        ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types)
            throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        try (PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8)) {
            writer.println("package com.interpreters.jlox.ast;");
            writer.println();
            writer.println("import java.util.List;");
            writer.println();
            writer.println("public abstract class " + baseName + " {");

            // The base accept() method.
            defineVisitor(writer, baseName, types);
            writer.println();
            writer.println("    public abstract <R> R accept(Visitor<R> visitor);");
            writer.println();

            for (String type : types) {
                String className = type.split(":")[0].trim();
                String fields = type.split(":")[1].trim();
                defineType(writer, baseName, className, fields);
            }
            writer.println("}");
        }
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println("    public static class " + className + " extends " + baseName + " {");

        // Fields.
        String[] fields;
        if (fieldList.isEmpty()) {
            fields = new String[0];
        } else {
            fields = fieldList.split(", ");
        }
        writer.println();
        for (String field : fields) {
            writer.println("        public final " + field + ";");
        }

        // Constructor.
        writer.println();
        writer.println("        public " + className + "(" + fieldList + ") {");

        // Store parameters in fields.
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }
        writer.println("        }");

        // Visitor pattern.
        writer.println();
        writer.println("        @Override");
        writer.println("        public <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");

        writer.println("    }");
        writer.println();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println();
        writer.println("    public interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" +
                    typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("    }");
    }
}

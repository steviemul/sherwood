package io.steviemul.sherwood.parsers.java;

import io.steviemul.sherwood.parsers.*;
import java.nio.file.Path;
import java.util.*;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;

/**
 * Extracts structural information from Java parse tree.
 * Visits ANTLR4 parse tree nodes to build method signatures and call relationships.
 */
public class JavaStructureVisitor extends Java20ParserBaseVisitor<Void> {

  private final Path filePath;
  @lombok.Getter
  private final List<MethodSignature> methods = new ArrayList<>();
  @lombok.Getter
  private final List<MethodCall> calls = new ArrayList<>();

  private String currentPackage = "";
  private final Deque<String> classStack = new ArrayDeque<>();
  private String currentMethod = "";
  private final List<String> currentAnnotations = new ArrayList<>();

  public JavaStructureVisitor(Path filePath) {
    this.filePath = filePath;
  }

  @Override
  public Void visitPackageDeclaration(Java20Parser.PackageDeclarationContext ctx) {
    if (ctx.identifier() != null && !ctx.identifier().isEmpty()) {
      currentPackage =
          ctx.identifier().stream()
              .map(RuleContext::getText)
              .reduce((a, b) -> a + "." + b)
              .orElse("");
    }
    return super.visitPackageDeclaration(ctx);
  }

  @Override
  public Void visitNormalClassDeclaration(Java20Parser.NormalClassDeclarationContext ctx) {
    if (ctx.typeIdentifier() != null) {
      classStack.push(ctx.typeIdentifier().getText());
      super.visitNormalClassDeclaration(ctx);
      classStack.pop();
    }
    return null;
  }

  @Override
  public Void visitAnnotation(Java20Parser.AnnotationContext ctx) {
    currentAnnotations.add(ctx.getText());
    return super.visitAnnotation(ctx);
  }

  @Override
  public Void visitMethodDeclaration(Java20Parser.MethodDeclarationContext ctx) {
    if (ctx.methodHeader() != null && ctx.methodHeader().methodDeclarator() != null) {
      String methodName = ctx.methodHeader().methodDeclarator().identifier().getText();
      List<String> parameters = extractParameters(ctx.methodHeader().methodDeclarator());
      currentMethod = getQualifiedName(methodName, parameters);

      Token start = ctx.getStart();
      Token stop = ctx.getStop();

      methods.add(
          new MethodSignature(
              methodName,
              currentMethod,
              start.getLine(),
              stop.getLine(),
              parameters,
              new ArrayList<>(currentAnnotations)));

      currentAnnotations.clear();
      super.visitMethodDeclaration(ctx);
      currentMethod = "";
    }
    return null;
  }

  @Override
  public Void visitMethodInvocation(Java20Parser.MethodInvocationContext ctx) {
    if (ctx.methodName() != null && ctx.methodName().unqualifiedMethodIdentifier() != null) {
      String methodName = ctx.methodName().unqualifiedMethodIdentifier().getText();
      int lineNumber = ctx.getStart().getLine();

      calls.add(new MethodCall(methodName, lineNumber, currentMethod));
    }
    return super.visitMethodInvocation(ctx);
  }

  private String getQualifiedName(String methodName, List<String> paramTypes) {
    StringBuilder sb = new StringBuilder();
    if (!currentPackage.isEmpty()) {
      sb.append(currentPackage).append(".");
    }
    if (!classStack.isEmpty()) {
      sb.append(String.join(".", classStack)).append(".");
    }
    sb.append(methodName);
    
    // Add parameter types to create unique signature
    sb.append("(");
    sb.append(String.join(",", paramTypes));
    sb.append(")");
    
    return sb.toString();
  }

  private List<String> extractParameters(Java20Parser.MethodDeclaratorContext ctx) {
    List<String> params = new ArrayList<>();
    
    if (ctx.receiverParameter() != null && ctx.receiverParameter().unannType() != null) {
      params.add(ctx.receiverParameter().unannType().getText());
    }
    
    if (ctx.formalParameterList() != null) {
      for (Java20Parser.FormalParameterContext param : ctx.formalParameterList().formalParameter()) {
        if (param.unannType() != null) {
          params.add(param.unannType().getText());
        } else if (param.variableArityParameter() != null 
                   && param.variableArityParameter().unannType() != null) {
          // Handle varargs like String...
          params.add(param.variableArityParameter().unannType().getText() + "...");
        }
      }
    }
    
    return params;
  }

  public String getPackageName() {
    return currentPackage;
  }
}

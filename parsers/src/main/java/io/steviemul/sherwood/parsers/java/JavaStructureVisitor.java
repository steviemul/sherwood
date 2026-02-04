package io.steviemul.sherwood.parsers.java;

import io.steviemul.sherwood.parsers.*;
import java.nio.file.Path;
import java.util.*;

import org.antlr.v4.runtime.ParserRuleContext;
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
  @lombok.Getter
  private final List<CodeBlock> codeBlocks = new ArrayList<>();

  private String currentPackage = "";
  private final Deque<String> classStack = new ArrayDeque<>();
  private String currentMethod = "";

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
  public Void visitMethodDeclaration(Java20Parser.MethodDeclarationContext ctx) {
    if (ctx.methodHeader() != null && ctx.methodHeader().methodDeclarator() != null) {
      String methodName = ctx.methodHeader().methodDeclarator().identifier().getText();
      List<String> parameters = extractParameters(ctx.methodHeader().methodDeclarator());
      
      // Save previous context before setting current method
      String previousMethod = currentMethod;
      currentMethod = getQualifiedName(methodName, parameters);

      // Extract annotations directly from methodModifier
      List<String> methodAnnotations = new ArrayList<>();
      Integer annotationStartLine = null;
      
      for (Java20Parser.MethodModifierContext modifier : ctx.methodModifier()) {
        if (modifier.annotation() != null) {
          methodAnnotations.add(modifier.annotation().getText());
          if (annotationStartLine == null) {
            annotationStartLine = modifier.getStart().getLine();
          }
        }
      }

      Token start = ctx.getStart();
      Token stop = ctx.getStop();

      // Use first annotation line as start if annotations are present
      int startLine = annotationStartLine != null ? annotationStartLine : start.getLine();

      // Extract source code from the token stream
      String sourceCode = "";
      if (start.getInputStream() != null) {
        sourceCode = start.getInputStream().getText(new org.antlr.v4.runtime.misc.Interval(start.getStartIndex(), stop.getStopIndex()));
      }

      MethodSignature method = new MethodSignature(
              methodName,
              currentMethod,
              startLine,
              stop.getLine(),
              parameters,
              methodAnnotations,
              sourceCode);

      methods.add(method);

      super.visitMethodDeclaration(ctx);
      currentMethod = previousMethod; // Restore previous context instead of clearing
    }

    return null;
  }

  @Override
  public Void visitMethodInvocation(Java20Parser.MethodInvocationContext ctx) {
    if (ctx.methodName() != null && ctx.methodName().unqualifiedMethodIdentifier() != null) {
      String methodName = ctx.methodName().unqualifiedMethodIdentifier().getText();
      int lineNumber = ctx.getStart().getLine();

      MethodCall call = new MethodCall(methodName, lineNumber, currentMethod);
      calls.add(call);
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

  @Override
  public Void visitStaticInitializer(Java20Parser.StaticInitializerContext ctx) {

    String sourceCode = getSourceCode(ctx);

    Token start = ctx.getStart();
    Token stop = ctx.getStop();

    String qualifiedName = getClassQualifiedName() + ".<clinit>";

    codeBlocks.add(
        new CodeBlock(
            CodeBlock.BlockType.STATIC_INITIALIZER,
            qualifiedName,
            start.getLine(),
            stop.getLine(),
            sourceCode));

    // Set context so method calls within this block are tracked
    String previousMethod = currentMethod;
    currentMethod = qualifiedName;
    super.visitStaticInitializer(ctx);
    currentMethod = previousMethod;

    return null;
  }

  private String getSourceCode(ParserRuleContext ctx) {

    Token start = ctx.getStart();
    Token stop = ctx.getStop();

    String sourceCode = "";

    if (start.getInputStream() != null) {
      sourceCode = start.getInputStream().getText(new org.antlr.v4.runtime.misc.Interval(start.getStartIndex(), stop.getStopIndex()));
    }

    return sourceCode;
  }

  @Override
  public Void visitInstanceInitializer(Java20Parser.InstanceInitializerContext ctx) {

    String sourceCode = getSourceCode(ctx);

    Token start = ctx.getStart();
    Token stop = ctx.getStop();

    String qualifiedName = getClassQualifiedName() + ".<init>";
    codeBlocks.add(
        new CodeBlock(
            CodeBlock.BlockType.INSTANCE_INITIALIZER,
            qualifiedName,
            start.getLine(),
            stop.getLine(),
            sourceCode));

    // Set context so method calls within this block are tracked
    String previousMethod = currentMethod;
    currentMethod = qualifiedName;
    super.visitInstanceInitializer(ctx);
    currentMethod = previousMethod;
    return null;
  }

  @Override
  public Void visitFieldDeclaration(Java20Parser.FieldDeclarationContext ctx) {
    // Check if field has initializer with method calls
    if (ctx.variableDeclaratorList() != null) {
      String sourceCode = getSourceCode(ctx);

      Token start = ctx.getStart();
      Token stop = ctx.getStop();

      // Determine if static or instance field
      boolean isStatic = ctx.parent != null 
          && ctx.parent.getText().contains("static");

      String qualifiedName = getClassQualifiedName() + ".<field>";
      CodeBlock.BlockType blockType = isStatic 
          ? CodeBlock.BlockType.STATIC_FIELD 
          : CodeBlock.BlockType.INSTANCE_FIELD;

      codeBlocks.add(
          new CodeBlock(
              blockType,
              qualifiedName,
              start.getLine(),
              stop.getLine(),
              sourceCode));

      // Set context so method calls in field initializers are tracked
      String previousMethod = currentMethod;
      currentMethod = qualifiedName;
      super.visitFieldDeclaration(ctx);
      currentMethod = previousMethod;
      return null;
    }

    return super.visitFieldDeclaration(ctx);
  }

  private String getClassQualifiedName() {
    StringBuilder sb = new StringBuilder();
    if (!currentPackage.isEmpty()) {
      sb.append(currentPackage).append(".");
    }
    if (!classStack.isEmpty()) {
      sb.append(String.join(".", classStack));
    }
    return sb.toString();
  }

  @Override
  public Void visitPrimaryNoNewArray(Java20Parser.PrimaryNoNewArrayContext ctx) {

    // Check if this is a method invocation: methodName '(' argumentList? ')' pNNA?
    if (ctx.methodName() != null) {
      String methodName = ctx.methodName().unqualifiedMethodIdentifier().getText();
      int lineNumber = ctx.getStart().getLine();
      
      // Add the method call
      MethodCall call = new MethodCall(methodName, lineNumber, currentMethod);
      calls.add(call);
    }
    
    return super.visitPrimaryNoNewArray(ctx);
  }
}

package io.steviemul.sherwood.parsers.java;

import io.steviemul.sherwood.parsers.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class JavaLanguageParser implements LanguageParser {

  @Override
  public ParsedFile parse(Path filePath) {
    try {
      CharStream input = CharStreams.fromPath(filePath);
      Java20Lexer lexer = new Java20Lexer(input);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      Java20Parser parser = new Java20Parser(tokens);

      ParseTree tree = parser.compilationUnit();

      JavaStructureVisitor visitor = new JavaStructureVisitor(filePath);
      visitor.visit(tree);

      return new ParsedFile(
          filePath,
          "java",
          visitor.getMethods(),
          visitor.getCalls(),
          Map.of("packageName", visitor.getPackageName()));
    } catch (IOException e) {
      throw new RuntimeException("Failed to parse " + filePath, e);
    }
  }

  @Override
  public List<MethodSignature> findEntryPoints(ParsedFile file) {
    return file.methods().stream().filter(this::isEntryPoint).toList();
  }

  private boolean isEntryPoint(MethodSignature method) {
    // Basic heuristics (can be extended later)
    return method.name().equals("main")
        || method.annotations().stream()
            .anyMatch(
                a ->
                    a.contains("RequestMapping")
                        || a.contains("GetMapping")
                        || a.contains("PostMapping")
                        || a.contains("PutMapping")
                        || a.contains("DeleteMapping")
                        || a.contains("PatchMapping"));
  }

  @Override
  public CallGraph buildCallGraph(List<ParsedFile> files) {
    CallGraph graph = new CallGraph();

    // Index all methods
    Map<String, MethodSignature> methodIndex = new HashMap<>();
    for (ParsedFile file : files) {
      for (MethodSignature method : file.methods()) {
        methodIndex.put(method.qualifiedName(), method);
      }
    }

    // Build edges based on calls (name-based matching)
    for (ParsedFile file : files) {
      for (MethodCall call : file.calls()) {
        // Simple name matching (can add type resolution later)
        methodIndex.values().stream()
            .filter(m -> m.name().equals(call.methodName()))
            .forEach(callee -> graph.addEdge(call.context(), callee.qualifiedName()));
      }
    }

    return graph;
  }

  @Override
  public ReachabilityResult isReachable(CallGraph graph, Location target) {
    // Find the method containing the target line
    // Then check if it's reachable from any entry point
    // This is a simplified implementation
    return new ReachabilityResult(false, null, List.of());
  }
}

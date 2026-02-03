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

    // Index all methods by qualified name (now includes parameter types)
    Map<String, MethodSignature> methodIndex = new HashMap<>();
    // Also index by simple name for fallback matching
    Map<String, List<MethodSignature>> methodsByName = new HashMap<>();
    
    for (ParsedFile file : files) {
      for (MethodSignature method : file.methods()) {
        methodIndex.put(method.qualifiedName(), method);
        methodsByName.computeIfAbsent(method.name(), k -> new ArrayList<>()).add(method);
      }
    }

    // Build edges based on calls
    for (ParsedFile file : files) {
      for (MethodCall call : file.calls()) {
        // Try to match by name - since we don't have argument types at call sites,
        // we match all methods with the same name (limitation of pure syntactic analysis)
        List<MethodSignature> candidates = methodsByName.get(call.methodName());
        if (candidates != null) {
          for (MethodSignature callee : candidates) {
            graph.addEdge(call.context(), callee.qualifiedName());
          }
        }
      }
    }

    return graph;
  }

  @Override
  public ReachabilityResult isReachable(CallGraph graph, Location target) {
    // This is a placeholder - typically you'd need to:
    // 1. Find which method contains the target line
    // 2. Check if that method is in the call graph
    // 3. Find entry points and trace reachability
    // For now, return not reachable
    return new ReachabilityResult(false, null, List.of());
  }

  /**
   * Find if a method containing the target line is reachable from entry points.
   *
   * @param files all parsed files in the project
   * @param target the target location to check
   * @return reachability result with entry point and path if reachable
   */
  public ReachabilityResult findReachability(List<ParsedFile> files, Location target) {
    // Build the call graph
    CallGraph graph = buildCallGraph(files);

    // Find the method containing the target line
    MethodSignature targetMethod = null;
    for (ParsedFile file : files) {
      if (file.filePath().equals(target.filePath())) {
        for (MethodSignature method : file.methods()) {
          if (target.lineNumber() >= method.startLine()
              && target.lineNumber() <= method.endLine()) {
            targetMethod = method;
            break;
          }
        }
      }
      if (targetMethod != null) break;
    }

    if (targetMethod == null) {
      return new ReachabilityResult(false, null, List.of());
    }

    // Find all entry points
    List<MethodSignature> entryPoints =
        files.stream().flatMap(f -> findEntryPoints(f).stream()).toList();

    // Check if target method is reachable from any entry point
    for (MethodSignature entryPoint : entryPoints) {
      Set<String> reachable = graph.findReachable(entryPoint.qualifiedName());

      if (reachable.contains(targetMethod.qualifiedName())) {
        // Found a path! Build it
        List<MethodSignature> path = buildPath(graph, entryPoint, targetMethod, files);
        return new ReachabilityResult(true, entryPoint, path);
      }
    }

    return new ReachabilityResult(false, null, List.of());
  }

  private List<MethodSignature> buildPath(
      CallGraph graph, MethodSignature start, MethodSignature target, List<ParsedFile> files) {
    // BFS to find shortest path
    Map<String, String> parent = new HashMap<>();
    Queue<String> queue = new LinkedList<>();
    Set<String> visited = new HashSet<>();

    queue.add(start.qualifiedName());
    visited.add(start.qualifiedName());
    parent.put(start.qualifiedName(), null);

    while (!queue.isEmpty()) {
      String current = queue.poll();

      if (current.equals(target.qualifiedName())) {
        // Reconstruct path
        List<String> pathNames = new ArrayList<>();
        String node = current;
        while (node != null) {
          pathNames.add(0, node);
          node = parent.get(node);
        }

        // Convert qualified names back to MethodSignatures
        Map<String, MethodSignature> methodIndex = new HashMap<>();
        for (ParsedFile file : files) {
          for (MethodSignature method : file.methods()) {
            methodIndex.put(method.qualifiedName(), method);
          }
        }

        return pathNames.stream()
            .map(methodIndex::get)
            .filter(Objects::nonNull)
            .toList();
      }

      for (String callee : graph.getCallees(current)) {
        if (visited.add(callee)) {
          parent.put(callee, current);
          queue.add(callee);
        }
      }
    }

    // No path found
    return List.of();
  }
}

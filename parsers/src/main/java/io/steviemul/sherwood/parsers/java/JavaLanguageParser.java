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
    return ReachabilityResult.notReachable();
  }

  /**
   * Find if a method containing the target line is reachable from entry points.
   *
   * @param files all parsed files in the project
   * @param target the target location to check
   * @return reachability result with confidence score and all paths
   */
  public ReachabilityResult findReachability(List<ParsedFile> files, Location target) {
    // Build the call graph
    CallGraph graph = buildCallGraph(files);

    // Find the method containing the target line
    MethodSignature targetMethod = findMethodContainingLine(files, target);
    if (targetMethod == null) {
      return ReachabilityResult.notReachable();
    }

    // Find all entry points
    List<MethodSignature> entryPoints =
        files.stream().flatMap(f -> findEntryPoints(f).stream()).toList();

    // First, check if reachable from any entry point (confidence = 1.0)
    List<List<MethodSignature>> pathsFromEntryPoints = new ArrayList<>();
    MethodSignature firstEntryPoint = null;

    for (MethodSignature entryPoint : entryPoints) {
      List<List<MethodSignature>> paths = findAllPaths(graph, entryPoint, targetMethod, files);
      if (!paths.isEmpty()) {
        if (firstEntryPoint == null) {
          firstEntryPoint = entryPoint;
        }
        pathsFromEntryPoints.addAll(paths);
      }
    }

    if (!pathsFromEntryPoints.isEmpty()) {
      // Reachable from entry point(s) - confidence 1.0
      return ReachabilityResult.reachableFromEntryPoint(
          firstEntryPoint, pathsFromEntryPoints.get(0), pathsFromEntryPoints);
    }

    // Not reachable from entry points, check if reachable from any other method (confidence = 0.5)
    // Build reverse graph to find what can reach the target
    Map<String, List<String>> callers = buildReverseGraph(graph);
    List<String> methodsThatCallTarget = callers.get(targetMethod.qualifiedName());

    if (methodsThatCallTarget != null && !methodsThatCallTarget.isEmpty()) {
      // Target is called by some method, so it's reachable (just not from entry points)
      // Find one path
      String callerName = methodsThatCallTarget.get(0);
      MethodSignature callerMethod = findMethodByQualifiedName(files, callerName);

      if (callerMethod != null) {
        List<MethodSignature> path = List.of(callerMethod, targetMethod);
        List<List<MethodSignature>> allPaths = new ArrayList<>();
        
        // Find all direct callers
        for (String caller : methodsThatCallTarget) {
          MethodSignature callerSig = findMethodByQualifiedName(files, caller);
          if (callerSig != null) {
            allPaths.add(List.of(callerSig, targetMethod));
          }
        }
        
        return ReachabilityResult.reachableFromNonEntryPoint(callerMethod, path, allPaths);
      }
    }

    // Not reachable at all - confidence 0.0
    return ReachabilityResult.notReachable();
  }

  private MethodSignature findMethodContainingLine(List<ParsedFile> files, Location target) {
    for (ParsedFile file : files) {
      if (file.filePath().equals(target.filePath())) {
        for (MethodSignature method : file.methods()) {
          if (target.lineNumber() >= method.startLine()
              && target.lineNumber() <= method.endLine()) {
            return method;
          }
        }
      }
    }
    return null;
  }

  private MethodSignature findMethodByQualifiedName(List<ParsedFile> files, String qualifiedName) {
    for (ParsedFile file : files) {
      for (MethodSignature method : file.methods()) {
        if (method.qualifiedName().equals(qualifiedName)) {
          return method;
        }
      }
    }
    return null;
  }

  private Map<String, List<String>> buildReverseGraph(CallGraph graph) {
    Map<String, List<String>> reverse = new HashMap<>();
    for (String caller : graph.getAllMethods()) {
      for (String callee : graph.getCallees(caller)) {
        reverse.computeIfAbsent(callee, k -> new ArrayList<>()).add(caller);
      }
    }
    return reverse;
  }

  private List<List<MethodSignature>> findAllPaths(
      CallGraph graph, MethodSignature start, MethodSignature target, List<ParsedFile> files) {
    List<List<MethodSignature>> allPaths = new ArrayList<>();
    List<String> currentPath = new ArrayList<>();
    Set<String> visited = new HashSet<>();

    findAllPathsDFS(
        graph, start.qualifiedName(), target.qualifiedName(), currentPath, visited, allPaths, files);

    return allPaths;
  }

  private void findAllPathsDFS(
      CallGraph graph,
      String current,
      String target,
      List<String> currentPath,
      Set<String> visited,
      List<List<MethodSignature>> allPaths,
      List<ParsedFile> files) {

    currentPath.add(current);
    visited.add(current);

    if (current.equals(target)) {
      // Found a path - convert to MethodSignatures
      List<MethodSignature> path = new ArrayList<>();
      for (String methodName : currentPath) {
        MethodSignature method = findMethodByQualifiedName(files, methodName);
        if (method != null) {
          path.add(method);
        }
      }
      if (!path.isEmpty()) {
        allPaths.add(path);
      }
    } else {
      // Continue searching
      for (String callee : graph.getCallees(current)) {
        if (!visited.contains(callee)) {
          findAllPathsDFS(graph, callee, target, currentPath, visited, allPaths, files);
        }
      }
    }

    currentPath.remove(currentPath.size() - 1);
    visited.remove(current);
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

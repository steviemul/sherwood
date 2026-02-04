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
      JavaStructureVisitor visitor = getJavaStructureVisitor(filePath, input);

      return new ParsedFile(
          filePath,
          "java",
          visitor.getMethods(),
          visitor.getCalls(),
          visitor.getCodeBlocks(),
          Map.of("packageName", visitor.getPackageName()));

    } catch (IOException e) {
      throw new RuntimeException("Failed to parse " + filePath, e);
    }
  }

  private static JavaStructureVisitor getJavaStructureVisitor(Path filePath, CharStream input) {
    Java20Lexer lexer = new Java20Lexer(input);
    // Remove default console error listener to suppress parse error messages
    lexer.removeErrorListeners();

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    Java20Parser parser = new Java20Parser(tokens);

    // Remove default console error listener to suppress parse error messages
    parser.removeErrorListeners();

    ParseTree tree = parser.compilationUnit();

    JavaStructureVisitor visitor = new JavaStructureVisitor(filePath);

    visitor.visit(tree);

    return visitor;
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

    Map<String, List<MethodSignature>> methodsByName = new HashMap<>();

    files.stream()
        .flatMap(f -> f.methods().stream())
        .forEach(method ->
            methodsByName.computeIfAbsent(method.name(), k -> new ArrayList<>()).add(method)
        );

    // Build edges based on calls
    files.stream()
        .flatMap(f -> f.calls().stream())
        .forEach(call -> {
          List<MethodSignature> candidates = methodsByName.get(call.methodName());
          if (candidates != null) {
            candidates.forEach(callee ->
                graph.addEdge(call.context(), callee.qualifiedName(), call.lineNumber())
            );
          }
        });

    return graph;
  }

  @Override
  public ReachabilityResult isReachable(CallGraph graph, Location target) {
    return ReachabilityResult.notReachable();
  }

  public ReachabilityResult findReachability(List<ParsedFile> files, Location target) {

    CallGraph graph = buildCallGraph(files);

    return findReachability(graph, files, target);
  }

  /**
   * Find if a method containing the target line is reachable from entry points.
   *
   * @param files all parsed files in the project
   * @param target the target location to check
   * @return reachability result with confidence score and all paths
   */
  public ReachabilityResult findReachability(CallGraph graph, List<ParsedFile> files, Location target) {

    // Check if the target file was parsed (i.e., it's a supported file type in the call graph)
    boolean targetFileInGraph =
        files.stream().anyMatch(f -> f.filePath().equals(target.filePath()));

    if (!targetFileInGraph) {
      // File not in call graph (e.g. no parser implemented for the file type)
      return ReachabilityResult.notCompleted();
    }

    // First, check if target is in a code block (initializer)
    CodeBlock targetBlock = findCodeBlockContainingLine(files, target);

    if (targetBlock != null) {
      String snippet = extractTargetSnippet(target);

      // Static initializers and fields run at class load - very high confidence
      if (targetBlock.type() == CodeBlock.BlockType.STATIC_INITIALIZER
          || targetBlock.type() == CodeBlock.BlockType.STATIC_FIELD) {

        // Create a pseudo-method signature for the static initializer
        MethodSignature clinit =
            new MethodSignature(
                "<clinit>",
                targetBlock.qualifiedName(),
                targetBlock.startLine(),
                targetBlock.endLine(),
                List.of(),
                List.of(),
                targetBlock.sourceCode());

        PathNode clinitNode = PathNode.entryPoint(clinit);

        return ReachabilityResult.reachableFromEntryPoint(
            clinit, List.of(clinitNode), List.of(List.of(clinitNode)), snippet);
      }

      // Instance initializers and fields run on construction - high confidence if constructor is called
      if (targetBlock.type() == CodeBlock.BlockType.INSTANCE_INITIALIZER
          || targetBlock.type() == CodeBlock.BlockType.INSTANCE_FIELD) {

        // Check if any constructor is reachable
        // For now, treat as reachable with 0.8 confidence (between entry point and internal)
        MethodSignature init =
            new MethodSignature(
                "<init>",
                targetBlock.qualifiedName(),
                targetBlock.startLine(),
                targetBlock.endLine(),
                List.of(),
                List.of(),
                targetBlock.sourceCode());

        PathNode initNode = PathNode.entryPoint(init);

        // We could enhance this to check if constructor is actually called
        return new ReachabilityResult(
            true, true, init, List.of(initNode), 0.8, List.of(List.of(initNode)), snippet);
      }
    }

    // Find the method containing the target line
    MethodSignature targetMethod = findMethodContainingLine(files, target);

    if (targetMethod == null) {
      return ReachabilityResult.notReachable();
    }

    // Find all entry points
    List<MethodSignature> entryPoints =
        files.stream().flatMap(f -> findEntryPoints(f).stream()).toList();

    // First, check if reachable from any entry point (confidence = 1.0)
    List<List<PathNode>> pathsFromEntryPoints = new ArrayList<>();
    MethodSignature firstEntryPoint = null;

    for (MethodSignature entryPoint : entryPoints) {
      List<List<PathNode>> paths = findAllPaths(graph, entryPoint, targetMethod, files);
      if (!paths.isEmpty()) {
        if (firstEntryPoint == null) {
          firstEntryPoint = entryPoint;
        }
        pathsFromEntryPoints.addAll(paths);
      }
    }

    if (!pathsFromEntryPoints.isEmpty()) {
      // Reachable from entry point(s) - confidence 1.0
      String snippet = extractTargetSnippet(target);
      return ReachabilityResult.reachableFromEntryPoint(
          firstEntryPoint, pathsFromEntryPoints.get(0), pathsFromEntryPoints, snippet);
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
        // Find the invocation line from the caller to the target
        Integer invokedAtLine = null;

        for (CallEdge edge : graph.getCallees(callerName)) {
          if (edge.callee().equals(targetMethod.qualifiedName())) {
            invokedAtLine = edge.invokedAtLine();
            break;
          }
        }
        
        List<PathNode> path = List.of(
            PathNode.entryPoint(callerMethod), 
            PathNode.invoked(targetMethod, invokedAtLine != null ? invokedAtLine : targetMethod.startLine())
        );

        List<List<PathNode>> allPaths = new ArrayList<>();
        
        // Find all direct callers with their invocation lines

        for (String caller : methodsThatCallTarget) {

          MethodSignature callerSig = findMethodByQualifiedName(files, caller);

          if (callerSig != null) {

            Integer invocationLine = null;

            for (CallEdge edge : graph.getCallees(caller)) {
              if (edge.callee().equals(targetMethod.qualifiedName())) {
                invocationLine = edge.invokedAtLine();
                break;
              }
            }

            allPaths.add(List.of(
                PathNode.entryPoint(callerSig), 
                PathNode.invoked(targetMethod, invocationLine != null ? invocationLine : targetMethod.startLine())
            ));
          }
        }
        
        String snippet = extractTargetSnippet(target);

        return ReachabilityResult.reachableFromNonEntryPoint(callerMethod, path, allPaths, snippet);
      }
    }

    // Not reachable at all - confidence 0.0
    return ReachabilityResult.notReachable();
  }

  /**
   * Extract source code snippet at the target location (with context lines).
   *
   * @param target the target location
   * @return the source code snippet (target line plus surrounding context)
   */
  private String extractTargetSnippet(Location target) {
    return extractTargetSnippet(target, 2); // 2 lines of context by default
  }

  /**
   * Extract source code snippet at the target location with specified context.
   *
   * @param target the target location
   * @param contextLines number of lines before and after to include
   * @return the source code snippet
   */
  private String extractTargetSnippet(Location target, int contextLines) {
    try {
      List<String> lines = Files.readAllLines(target.filePath());
      int targetLineIdx = target.lineNumber() - 1; // Convert to 0-based
      
      int startIdx = Math.max(0, targetLineIdx - contextLines);
      int endIdx = Math.min(lines.size(), targetLineIdx + contextLines + 1);
      
      StringBuilder snippet = new StringBuilder();

      for (int i = startIdx; i < endIdx; i++) {
        snippet.append(String.format("%4d: %s%n", i + 1, lines.get(i)));
      }
      
      return snippet.toString();
    } catch (IOException e) {
      return ""; // Return empty string if unable to read
    }
  }

  private MethodSignature findMethodContainingLine(List<ParsedFile> files, Location target) {

    return files.stream()
        .filter(f -> f.filePath().equals(target.filePath()))
        .flatMap(f -> f.methods().stream())
        .filter(m -> target.lineNumber() >= m.startLine() && target.lineNumber() <= m.endLine())
        .findFirst()
        .orElse(null);

  }

  private CodeBlock findCodeBlockContainingLine(List<ParsedFile> files, Location target) {
    return files.stream()
        .filter(f -> f.filePath().equals(target.filePath()))
        .flatMap(f -> f.codeBlocks().stream())
        .filter(b -> target.lineNumber() >= b.startLine() && target.lineNumber() <= b.endLine())
        .findFirst()
        .orElse(null);
  }

  private MethodSignature findMethodByQualifiedName(List<ParsedFile> files, String qualifiedName) {
    return files.stream()
        .flatMap(f -> f.methods().stream())
        .filter(m -> m.qualifiedName().equals(qualifiedName))
        .findFirst()
        .orElse(null);
  }

  private Map<String, List<String>> buildReverseGraph(CallGraph graph) {
    Map<String, List<String>> reverse = new HashMap<>();
    for (String caller : graph.getAllMethods()) {
      for (CallEdge edge : graph.getCallees(caller)) {
        reverse.computeIfAbsent(edge.callee(), k -> new ArrayList<>()).add(caller);
      }
    }
    return reverse;
  }

  private List<List<PathNode>> findAllPaths(
      CallGraph graph, MethodSignature start, MethodSignature target, List<ParsedFile> files) {
    List<List<PathNode>> allPaths = new ArrayList<>();
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
      List<List<PathNode>> allPaths,
      List<ParsedFile> files) {

    currentPath.add(current);
    visited.add(current);

    if (current.equals(target)) {
      // Found a path - convert to PathNodes with invocation line numbers
      List<PathNode> path = new ArrayList<>();

      for (int i = 0; i < currentPath.size(); i++) {

        String methodName = currentPath.get(i);
        MethodSignature method = findMethodByQualifiedName(files, methodName);

        if (method != null) {
          if (i == 0) {
            // First method in path (entry point) - no invocation line
            path.add(PathNode.entryPoint(method));
          } else {
            // Find the invocation line from the previous method
            String caller = currentPath.get(i - 1);
            Integer invokedAtLine = null;

            for (CallEdge edge : graph.getCallees(caller)) {
              if (edge.callee().equals(methodName)) {
                invokedAtLine = edge.invokedAtLine();
                break;
              }
            }

            path.add(PathNode.invoked(method, invokedAtLine != null ? invokedAtLine : method.startLine()));
          }
        }
      }

      if (!path.isEmpty()) {
        allPaths.add(path);
      }
    } else {

      // Continue searching
      for (CallEdge edge : graph.getCallees(current)) {
        if (!visited.contains(edge.callee())) {
          findAllPathsDFS(graph, edge.callee(), target, currentPath, visited, allPaths, files);
        }
      }
    }

    currentPath.remove(currentPath.size() - 1);
    visited.remove(current);
  }
}

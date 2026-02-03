package io.steviemul.sherwood.parsers;

import io.steviemul.sherwood.sarif.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Maps SARIF results to source code and performs reachability analysis. */
public class ReachabilityAnalyzer {

  private final Map<String, LanguageParser> languageParsers;

  public ReachabilityAnalyzer() {
    this.languageParsers =
        Map.of(
            "java", new io.steviemul.sherwood.parsers.java.JavaLanguageParser()
            // Add Python, JavaScript parsers later
            );
  }

  /**
   * Analyze if a SARIF result location is reachable from entry points.
   *
   * @param result SARIF result to analyze
   * @param sourceRoot root directory of the source code
   * @return reachability analysis result
   */
  public ReachabilityResult analyzeResult(Result result, Path sourceRoot) {
    if (result.getLocations() == null || result.getLocations().isEmpty()) {
      return new ReachabilityResult(false, null, List.of());
    }

    // Extract target location from result
    io.steviemul.sherwood.sarif.Location targetLocation = result.getLocations().get(0);
    String uri = targetLocation.getPhysicalLocation().getArtifactLocation().getUri().toString();
    int targetLine = targetLocation.getPhysicalLocation().getRegion().getStartLine().intValue();

    // Determine language from file extension
    String language = detectLanguage(uri);
    LanguageParser parser = languageParsers.get(language);

    if (parser == null) {
      return new ReachabilityResult(false, null, List.of());
    }

    // Parse all files in project
    List<ParsedFile> files = parseProject(sourceRoot, parser);

    // Build call graph
    CallGraph graph = parser.buildCallGraph(files);

    // Find entry points
    List<MethodSignature> entryPoints =
        files.stream().flatMap(f -> parser.findEntryPoints(f).stream()).toList();

    // Check reachability from each entry point
    for (MethodSignature entry : entryPoints) {
      Set<String> reachable = graph.findReachable(entry.qualifiedName());

      // Check if any reachable method contains target line
      for (ParsedFile file : files) {
        if (file.filePath().toString().endsWith(uri)) {
          for (MethodSignature method : file.methods()) {
            if (reachable.contains(method.qualifiedName())
                && targetLine >= method.startLine()
                && targetLine <= method.endLine()) {

              return new ReachabilityResult(true, entry, buildPath(graph, entry, method));
            }
          }
        }
      }
    }

    return new ReachabilityResult(false, null, List.of());
  }

  private List<ParsedFile> parseProject(Path root, LanguageParser parser) {
    List<ParsedFile> files = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(root)) {
      paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".java")) // Extend for other languages
          .forEach(
              path -> {
                try {
                  files.add(parser.parse(path));
                } catch (java.lang.Exception e) {
                  // Log and continue
                  System.err.println("Failed to parse " + path + ": " + e.getMessage());
                }
              });
    } catch (IOException e) {
      throw new RuntimeException("Failed to walk directory tree", e);
    }

    return files;
  }

  private String detectLanguage(String uri) {
    if (uri.endsWith(".java")) return "java";
    if (uri.endsWith(".py")) return "python";
    if (uri.endsWith(".js")) return "javascript";
    return "unknown";
  }

  private List<MethodSignature> buildPath(
      CallGraph graph, MethodSignature start, MethodSignature target) {
    // BFS to find shortest path for CodeFlow visualization
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

        // Convert to MethodSignature list (simplified - would need method lookup)
        return List.of(start, target);
      }

      for (String callee : graph.getCallees(current)) {
        if (visited.add(callee)) {
          parent.put(callee, current);
          queue.add(callee);
        }
      }
    }

    return List.of(); // No path found
  }
}

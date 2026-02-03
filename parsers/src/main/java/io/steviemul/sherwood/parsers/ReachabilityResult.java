package io.steviemul.sherwood.parsers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Result of reachability analysis.
 *
 * @param isReachable whether the target is reachable from an entry point
 * @param entryPoint the entry point from which the target is reachable (null if not reachable)
 * @param path list of path nodes in the call path from entry point to target
 * @param confidence confidence score: 1.0 = reachable from entry point, 0.5 = reachable from
 *     non-entry method, 0.0 = not reachable
 * @param allPaths all paths found to the target (from entry points if confidence=1.0, from any
 *     method if confidence=0.5)
 * @param targetSnippet the source code snippet at the target location (for display/comparison with
 *     SARIF snippets)
 */
public record ReachabilityResult(
    boolean isReachable,
    MethodSignature entryPoint,
    List<PathNode> path,
    double confidence,
    List<List<PathNode>> allPaths,
    String targetSnippet) {

  /**
   * Create a result indicating the target is not reachable.
   *
   * @return not reachable result with confidence 0.0
   */
  public static ReachabilityResult notReachable() {
    return new ReachabilityResult(false, null, List.of(), 0.0, List.of(), "");
  }

  /**
   * Create a result indicating the target is reachable from an entry point.
   *
   * @param entryPoint the entry point method
   * @param path the shortest path from entry point to target
   * @param allPaths all paths from entry points to target
   * @param targetSnippet the source code at the target location
   * @return reachable result with confidence 1.0
   */
  public static ReachabilityResult reachableFromEntryPoint(
      MethodSignature entryPoint,
      List<PathNode> path,
      List<List<PathNode>> allPaths,
      String targetSnippet) {
    return new ReachabilityResult(true, entryPoint, path, 1.0, allPaths, targetSnippet);
  }

  /**
   * Create a result indicating the target is reachable but not from any entry point.
   *
   * @param callingMethod a method that can reach the target
   * @param path a path to the target
   * @param allPaths all paths found to the target
   * @param targetSnippet the source code at the target location
   * @return reachable result with confidence 0.5
   */
  public static ReachabilityResult reachableFromNonEntryPoint(
      MethodSignature callingMethod,
      List<PathNode> path,
      List<List<PathNode>> allPaths,
      String targetSnippet) {
    return new ReachabilityResult(true, callingMethod, path, 0.5, allPaths, targetSnippet);
  }

  /**
   * Generate a DOT (Graphviz) representation of the reachability paths.
   *
   * @return DOT format string showing all paths to the target
   */
  public String toDot() {
    if (!isReachable || allPaths.isEmpty()) {
      return "digraph ReachabilityGraph {\n  label=\"No reachable paths\";\n}\n";
    }

    StringBuilder dot = new StringBuilder();
    dot.append("digraph ReachabilityGraph {\n");
    dot.append("  rankdir=TD;\n");
    dot.append("  node [shape=box, style=rounded];\n\n");

    // Collect all unique edges from all paths
    Map<String, String> edges = new HashMap<>();
    for (List<PathNode> path : allPaths) {
      for (int i = 0; i < path.size() - 1; i++) {
        String from = path.get(i).format();
        String to = path.get(i + 1).format();
        String edge = "  \"" + escapeDot(from) + "\" -> \"" + escapeDot(to) + "\";";
        edges.put(from + "->" + to, edge);
      }
    }

    // Add edge from last node to code snippet if available
    if (!allPaths.isEmpty() && !targetSnippet.isEmpty()) {
      PathNode lastNode = allPaths.get(0).get(allPaths.get(0).size() - 1);
      String lastNodeLabel = lastNode.format();
      String snippetLine = getFirstCodeLine(targetSnippet);
      String snippetEdge =
          "  \"" + escapeDot(lastNodeLabel) + "\" -> \"" + escapeDot(snippetLine) + "\";";
      edges.put(lastNodeLabel + "->snippet", snippetEdge);
    }

    // Add all unique edges
    for (String edge : edges.values().stream().sorted().toList()) {
      dot.append(edge).append("\n");
    }

    dot.append("}\n");
    return dot.toString();
  }

  /**
   * Generate a Mermaid diagram representation of the reachability paths.
   *
   * @return Mermaid format string showing all paths to the target
   */
  public String toMermaid() {
    if (!isReachable || allPaths.isEmpty()) {
      return "graph TD\n  N0[\"No reachable paths\"]\n";
    }

    StringBuilder mermaid = new StringBuilder();
    mermaid.append("graph TD\n");

    // Create node IDs and declarations
    Map<String, String> nodeIds = new HashMap<>();
    int nodeCounter = 0;

    for (List<PathNode> path : allPaths) {
      for (PathNode node : path) {
        String nodeKey = node.method().qualifiedName();
        if (!nodeIds.containsKey(nodeKey)) {
          String nodeId = "N" + nodeCounter++;
          nodeIds.put(nodeKey, nodeId);
          mermaid
              .append("  ")
              .append(nodeId)
              .append("[\"")
              .append(escapeMermaid(node.format()))
              .append("\"]\n");
        }
      }
    }

    // Add code snippet node if available
    String snippetNodeId = null;
    if (!targetSnippet.isEmpty()) {
      snippetNodeId = "N" + nodeCounter++;
      String snippetLabel = getFirstCodeLine(targetSnippet);
      mermaid
          .append("  ")
          .append(snippetNodeId)
          .append("[\"")
          .append(escapeMermaid(snippetLabel))
          .append("\"]\n");
    }

    mermaid.append("\n");

    // Add edges
    Map<String, String> edges = new HashMap<>();
    for (List<PathNode> path : allPaths) {
      for (int i = 0; i < path.size() - 1; i++) {
        String from = path.get(i).method().qualifiedName();
        String to = path.get(i + 1).method().qualifiedName();
        String edge = "  " + nodeIds.get(from) + " --> " + nodeIds.get(to);
        edges.put(from + "->" + to, edge);
      }
    }

    // Add edge from last node to code snippet
    if (snippetNodeId != null && !allPaths.isEmpty()) {
      PathNode lastNode = allPaths.get(0).get(allPaths.get(0).size() - 1);
      String lastNodeKey = lastNode.method().qualifiedName();
      String snippetEdge = "  " + nodeIds.get(lastNodeKey) + " --> " + snippetNodeId;
      edges.put(lastNodeKey + "->snippet", snippetEdge);
    }

    // Add edge from last node to code snippet
    if (snippetNodeId != null && !allPaths.isEmpty()) {
      PathNode lastNode = allPaths.get(0).get(allPaths.get(0).size() - 1);
      String lastNodeKey = lastNode.method().qualifiedName();
      String snippetEdge = "  " + nodeIds.get(lastNodeKey) + " --> " + snippetNodeId;
      edges.put(lastNodeKey + "->snippet", snippetEdge);
    }

    for (String edge : edges.values().stream().sorted().collect(Collectors.toList())) {
      mermaid.append(edge).append("\n");
    }

    return mermaid.toString();
  }

  /**
   * Generate a human-readable text representation of the reachability paths.
   *
   * @return text format string showing all paths to the target
   */
  public String toText() {
    if (!isReachable || allPaths.isEmpty()) {
      return "Reachability Paths:\n===================\n\nNo reachable paths found.\n";
    }

    StringBuilder text = new StringBuilder();
    text.append("Reachability Paths:\n");
    text.append("===================\n");
    text.append("Confidence: ").append(confidence).append("\n");
    text.append("Entry Point: ")
        .append(entryPoint != null ? entryPoint.qualifiedName() : "N/A")
        .append("\n\n");

    for (int i = 0; i < allPaths.size(); i++) {
      List<PathNode> path = allPaths.get(i);
      text.append("Path ").append(i + 1).append(":\n");
      for (int j = 0; j < path.size(); j++) {
        text.append("  ").append("  ".repeat(j)).append(path.get(j).format());
        if (j < path.size() - 1) {
          text.append(" →\n");
        } else {
          text.append("\n");
        }
      }
      text.append("\n");
    }

    return text.toString();
  }

  /**
   * Get the target line from the snippet (the actual line where the issue is, not context lines).
   * The snippet is formatted with context lines before and after, so the target is typically in the
   * middle.
   *
   * @param snippet the code snippet with line numbers and context
   * @return the target line with code, or empty string if none found
   */
  private static String getFirstCodeLine(String snippet) {
    if (snippet == null || snippet.isEmpty()) {
      return "";
    }
    // The snippet has contextLines before and after the target
    // With default context of 2, the snippet has 5 lines and target is line 3 (index 2)
    // Get the middle line which is the actual target line
    List<String> lines = snippet.lines().filter(line -> !line.trim().isEmpty()).toList();
    if (lines.isEmpty()) {
      return "";
    }
    // Return the middle line (the target line)
    int middleIndex = lines.size() / 2;
    return lines.get(middleIndex);
  }

  private static String escapeDot(String text) {
    return text.replace("\"", "\\\"").replace("\n", "\\n");
  }

  private static String escapeMermaid(String text) {
    return text.replace("\"", "&quot;").replace("[", "&#91;").replace("]", "&#93;");
  }
}

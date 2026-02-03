package io.steviemul.sherwood.parsers;

import java.util.*;

/** Directed graph representing method call relationships. Edges point from caller to callee. */
public class CallGraph {
  private final Map<String, Set<CallEdge>> adjacencyList = new HashMap<>();

  /**
   * Add a call edge from caller to callee.
   *
   * @param caller qualified name of the calling method
   * @param callee qualified name of the called method
   * @param invokedAtLine line number where the call occurs
   */
  public void addEdge(String caller, String callee, int invokedAtLine) {
    adjacencyList
        .computeIfAbsent(caller, k -> new HashSet<>())
        .add(new CallEdge(callee, invokedAtLine));
  }

  /**
   * Get all methods directly called by the given method.
   *
   * @param method qualified method name
   * @return set of call edges
   */
  public Set<CallEdge> getCallees(String method) {
    return adjacencyList.getOrDefault(method, Set.of());
  }

  /**
   * Find all methods reachable from a given starting point using BFS.
   *
   * @param startMethod qualified name of the starting method
   * @return set of all reachable method names (including startMethod)
   */
  public Set<String> findReachable(String startMethod) {
    Set<String> visited = new HashSet<>();
    Queue<String> queue = new LinkedList<>();
    queue.add(startMethod);

    while (!queue.isEmpty()) {
      String current = queue.poll();
      if (visited.add(current)) {
        for (CallEdge edge : getCallees(current)) {
          queue.add(edge.callee());
        }
      }
    }

    return visited;
  }

  /**
   * Get all methods in the graph.
   *
   * @return set of all method names
   */
  public Set<String> getAllMethods() {
    return adjacencyList.keySet();
  }

  /**
   * Export the call graph to DOT format (Graphviz). Can be visualized using Graphviz tools or
   * online at <a href="https://dreampuf.github.io/GraphvizOnline/">...</a>
   *
   * @return DOT format representation of the graph
   */
  public String toDot() {
    StringBuilder dot = new StringBuilder();
    dot.append("digraph CallGraph {\n");
    dot.append("  rankdir=LR;\n");
    dot.append("  node [shape=box, style=rounded];\n\n");

    // Add all nodes and edges
    for (Map.Entry<String, Set<CallEdge>> entry : adjacencyList.entrySet()) {
      String caller = escapeForDot(entry.getKey());

      for (CallEdge edge : entry.getValue()) {
        String calleeEscaped = escapeForDot(edge.callee());
        dot.append("  \"").append(caller).append("\" -> \"").append(calleeEscaped).append("\";\n");
      }
    }

    dot.append("}\n");
    return dot.toString();
  }

  /**
   * Export the call graph to Mermaid format. Can be visualized in GitHub markdown, VS Code, or at
   * <a href="https://mermaid.live/">...</a>
   *
   * @return Mermaid format representation of the graph
   */
  public String toMermaid() {
    StringBuilder mermaid = new StringBuilder();
    mermaid.append("graph LR\n");

    // Create node IDs (mermaid needs short IDs)
    Map<String, String> nodeIds = new HashMap<>();
    int nodeCounter = 0;
    for (String method : adjacencyList.keySet()) {
      nodeIds.put(method, "N" + nodeCounter++);
    }
    for (Set<CallEdge> callees : adjacencyList.values()) {
      for (CallEdge edge : callees) {
        if (!nodeIds.containsKey(edge.callee())) {
          nodeIds.put(edge.callee(), "N" + nodeCounter++);
        }
      }
    }

    // Add node definitions with labels
    for (Map.Entry<String, String> entry : nodeIds.entrySet()) {
      String method = entry.getKey();
      String nodeId = entry.getValue();
      String label = escapeForMermaid(method);
      mermaid.append("  ").append(nodeId).append("[\"").append(label).append("\"]\n");
    }

    mermaid.append("\n");

    // Add edges
    for (Map.Entry<String, Set<CallEdge>> entry : adjacencyList.entrySet()) {
      String callerId = nodeIds.get(entry.getKey());

      for (CallEdge edge : entry.getValue()) {
        String calleeId = nodeIds.get(edge.callee());
        mermaid.append("  ").append(callerId).append(" --> ").append(calleeId).append("\n");
      }
    }

    return mermaid.toString();
  }

  /**
   * Export a simple text representation of the call graph.
   *
   * @return text representation of the graph
   */
  public String toText() {
    StringBuilder text = new StringBuilder();
    text.append("Call Graph:\n");
    text.append("===========\n\n");

    List<String> sortedMethods = new ArrayList<>(adjacencyList.keySet());
    Collections.sort(sortedMethods);

    for (String caller : sortedMethods) {
      text.append(caller).append("\n");
      Set<CallEdge> callees = adjacencyList.get(caller);
      if (callees != null && !callees.isEmpty()) {
        List<String> sortedCallees = new ArrayList<>();
        for (CallEdge edge : callees) {
          sortedCallees.add(edge.callee());
        }
        Collections.sort(sortedCallees);
        for (String callee : sortedCallees) {
          text.append("  -> ").append(callee).append("\n");
        }
      }
      text.append("\n");
    }

    return text.toString();
  }

  private String escapeForDot(String s) {
    // Escape quotes and backslashes for DOT format
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private String escapeForMermaid(String s) {
    // Escape quotes for Mermaid format
    return s.replace("\"", "&quot;");
  }
}

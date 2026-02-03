package io.steviemul.sherwood.parsers;

import java.util.*;

/** Directed graph representing method call relationships. Edges point from caller to callee. */
public class CallGraph {
  private final Map<String, Set<String>> adjacencyList = new HashMap<>();

  /**
   * Add a call edge from caller to callee.
   *
   * @param caller qualified name of the calling method
   * @param callee qualified name of the called method
   */
  public void addEdge(String caller, String callee) {
    adjacencyList.computeIfAbsent(caller, k -> new HashSet<>()).add(callee);
  }

  /**
   * Get all methods directly called by the given method.
   *
   * @param method qualified method name
   * @return set of called methods
   */
  public Set<String> getCallees(String method) {
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
        queue.addAll(getCallees(current));
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
}

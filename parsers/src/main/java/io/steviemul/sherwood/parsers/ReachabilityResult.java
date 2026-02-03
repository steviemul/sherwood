package io.steviemul.sherwood.parsers;

import java.util.List;

/**
 * Result of reachability analysis.
 *
 * @param isReachable whether the target is reachable from an entry point
 * @param entryPoint the entry point from which the target is reachable (null if not reachable)
 * @param path list of methods in the call path from entry point to target
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
    List<MethodSignature> path,
    double confidence,
    List<List<MethodSignature>> allPaths,
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
      List<MethodSignature> path,
      List<List<MethodSignature>> allPaths,
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
      List<MethodSignature> path,
      List<List<MethodSignature>> allPaths,
      String targetSnippet) {
    return new ReachabilityResult(true, callingMethod, path, 0.5, allPaths, targetSnippet);
  }
}

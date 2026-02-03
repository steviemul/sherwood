package io.steviemul.sherwood.parsers;

import java.util.List;

/**
 * Result of reachability analysis.
 *
 * @param isReachable whether the target is reachable from an entry point
 * @param entryPoint the entry point from which the target is reachable (null if not reachable)
 * @param path list of methods in the call path from entry point to target
 */
public record ReachabilityResult(
    boolean isReachable, MethodSignature entryPoint, List<MethodSignature> path) {}

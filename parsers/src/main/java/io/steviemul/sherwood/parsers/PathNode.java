package io.steviemul.sherwood.parsers;

/**
 * Represents a method in a call path with its invocation context.
 *
 * @param method the method signature
 * @param invokedAtLine line number where this method was invoked (null for entry point/first in
 *     path)
 */
public record PathNode(MethodSignature method, Integer invokedAtLine) {

  /**
   * Create a PathNode for an entry point (no invocation line).
   *
   * @param method the entry point method
   * @return PathNode with null invokedAtLine
   */
  public static PathNode entryPoint(MethodSignature method) {
    return new PathNode(method, null);
  }

  /**
   * Create a PathNode for a called method with its invocation line.
   *
   * @param method the called method
   * @param invokedAtLine the line where it was invoked
   * @return PathNode with invocation line
   */
  public static PathNode invoked(MethodSignature method, int invokedAtLine) {
    return new PathNode(method, invokedAtLine);
  }

  /**
   * Format the node for display: methodName : L10 or methodName : L10 (invoked at L48).
   *
   * @return formatted string
   */
  public String format() {
    String base = method.qualifiedName() + " : L" + method.startLine();
    if (invokedAtLine != null) {
      base += " (invoked at L" + invokedAtLine + ")";
    }
    return base;
  }
}

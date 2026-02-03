package io.steviemul.sherwood.parsers;

/**
 * Represents an edge in the call graph - a call from one method to another.
 *
 * @param callee qualified name of the called method
 * @param invokedAtLine line number where this method is invoked in the caller
 */
public record CallEdge(String callee, int invokedAtLine) {}

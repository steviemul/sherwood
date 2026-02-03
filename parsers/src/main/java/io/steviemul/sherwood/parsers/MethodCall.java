package io.steviemul.sherwood.parsers;

/**
 * Represents a method/function invocation (without type resolution).
 *
 * @param methodName name of the called method
 * @param lineNumber line where the call occurs
 * @param context qualified name of the method containing this call
 */
public record MethodCall(String methodName, int lineNumber, String context) {}

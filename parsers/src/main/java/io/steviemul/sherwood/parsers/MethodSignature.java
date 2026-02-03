package io.steviemul.sherwood.parsers;

import java.util.List;

/**
 * Represents a method/function declaration.
 *
 * @param name simple method name
 * @param qualifiedName fully qualified name (e.g., "com.example.MyClass.myMethod")
 * @param startLine starting line number (1-based)
 * @param endLine ending line number (1-based)
 * @param parameters list of parameter names or types
 * @param annotations list of annotations/decorators (e.g., "@RestController", "@Override")
 */
public record MethodSignature(
    String name,
    String qualifiedName,
    int startLine,
    int endLine,
    List<String> parameters,
    List<String> annotations) {}

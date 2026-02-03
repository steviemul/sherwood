package io.steviemul.sherwood.parsers;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Language-agnostic representation of a parsed source file.
 *
 * @param filePath path to the source file
 * @param language programming language (e.g., "java", "python", "javascript")
 * @param methods list of methods/functions declared in the file
 * @param calls list of method/function calls made in the file
 * @param codeBlocks list of non-method code blocks (initializers, field initializers)
 * @param metadata language-specific metadata (e.g., package name, imports)
 */
public record ParsedFile(
    Path filePath,
    String language,
    List<MethodSignature> methods,
    List<MethodCall> calls,
    List<CodeBlock> codeBlocks,
    Map<String, Object> metadata) {}

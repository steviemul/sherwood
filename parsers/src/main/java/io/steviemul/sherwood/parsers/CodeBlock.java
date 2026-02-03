package io.steviemul.sherwood.parsers;

/**
 * Represents a code block that is not a method (e.g., static initializer, instance initializer,
 * field initializer).
 *
 * @param type the type of block (STATIC_INITIALIZER, INSTANCE_INITIALIZER, STATIC_FIELD,
 *     INSTANCE_FIELD)
 * @param qualifiedName fully qualified name (e.g., "com.example.MyClass.<clinit>")
 * @param startLine starting line number (1-based)
 * @param endLine ending line number (1-based)
 * @param sourceCode the actual source code of the block
 */
public record CodeBlock(
    BlockType type, String qualifiedName, int startLine, int endLine, String sourceCode) {

  public enum BlockType {
    STATIC_INITIALIZER, // static { ... }
    INSTANCE_INITIALIZER, // { ... }
    STATIC_FIELD, // static field = value;
    INSTANCE_FIELD // field = value;
  }
}

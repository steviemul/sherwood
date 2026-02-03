package io.steviemul.sherwood.parsers;

import java.nio.file.Path;

/**
 * Represents a location in source code.
 *
 * @param filePath path to the source file
 * @param lineNumber line number (1-based)
 */
public record Location(Path filePath, int lineNumber) {}

package io.steviemul.sherwood.parsers;

import java.nio.file.Path;
import java.util.List;

/**
 * Base interface for language-specific parsers. Implementations should handle ANTLR4-based parsing
 * for different programming languages.
 */
public interface LanguageParser {
  /**
   * Parse a source file and extract its structure.
   *
   * @param filePath path to the source file
   * @return parsed representation of the file
   */
  ParsedFile parse(Path filePath);

  /**
   * Find all potential entry points (main methods, HTTP endpoints, etc.).
   *
   * @param file parsed file to analyze
   * @return list of entry point methods
   */
  List<MethodSignature> findEntryPoints(ParsedFile file);

  /**
   * Build call graph for reachability analysis.
   *
   * @param files list of parsed files
   * @return call graph representing method invocations
   */
  CallGraph buildCallGraph(List<ParsedFile> files);

  /**
   * Check if a line/region is reachable from entry points.
   *
   * @param graph call graph
   * @param target target location to check
   * @return reachability analysis result
   */
  ReachabilityResult isReachable(CallGraph graph, Location target);

  ReachabilityResult findReachability(List<ParsedFile> files, Location target);

  ReachabilityResult findReachability(CallGraph graph, List<ParsedFile> files, Location target);
}

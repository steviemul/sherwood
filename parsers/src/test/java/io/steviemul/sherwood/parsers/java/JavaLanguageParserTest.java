package io.steviemul.sherwood.parsers.java;

import static org.assertj.core.api.Assertions.*;

import io.steviemul.sherwood.parsers.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

class JavaLanguageParserTest {

  @Test
  void testReachabilityAnalysis() {
    // Given: A sample Java file with main -> processRequest -> validateInput ->
    // executeLogic -> performCalculation
    Path testFile =
        Paths.get("src/test/resources/SampleApp.java").toAbsolutePath().normalize();
    JavaLanguageParser parser = new JavaLanguageParser();

    // Parse the file
    ParsedFile parsedFile = parser.parse(testFile);

    // Verify we found the methods
    assertThat(parsedFile.methods())
        .hasSize(6) // main, processRequest, validateInput, executeLogic, performCalculation,
        // unreachableMethod
        .extracting(MethodSignature::name)
        .contains("main", "processRequest", "performCalculation", "unreachableMethod");

    // Verify we found the method calls
    assertThat(parsedFile.calls()).hasSizeGreaterThan(0);

    // When: We check if performCalculation (line 23, inside the method body) is reachable from
    // main
    Location targetLocation = new Location(testFile, 23);
    ReachabilityResult result = parser.findReachability(List.of(parsedFile), targetLocation);

    // Then: It should be reachable
    assertThat(result.isReachable()).isTrue();
    assertThat(result.entryPoint()).isNotNull();
    assertThat(result.entryPoint().name()).isEqualTo("main");

    // The path should include: main -> processRequest -> executeLogic -> performCalculation
    assertThat(result.path())
        .hasSize(4)
        .extracting(MethodSignature::name)
        .containsExactly("main", "processRequest", "executeLogic", "performCalculation");

    // When: We check if unreachableMethod (line 28, inside the method body) is reachable from
    // main
    Location unreachableLocation = new Location(testFile, 28);
    ReachabilityResult unreachableResult =
        parser.findReachability(List.of(parsedFile), unreachableLocation);

    // Then: It should NOT be reachable
    assertThat(unreachableResult.isReachable()).isFalse();
    assertThat(unreachableResult.entryPoint()).isNull();
    assertThat(unreachableResult.path()).isEmpty();
  }

  @Test
  void testOverloadedMethods() {
    // Given: A Java file with overloaded methods
    Path testFile =
        Paths.get("src/test/resources/OverloadedApp.java").toAbsolutePath().normalize();
    JavaLanguageParser parser = new JavaLanguageParser();

    // Parse the file
    ParsedFile parsedFile = parser.parse(testFile);

    // Verify we found both overloaded methods with different signatures
    assertThat(parsedFile.methods())
        .hasSize(3) // main, process(String), process(String,int)
        .extracting(MethodSignature::qualifiedName)
        .contains(
            "com.example.overload.OverloadedApp.main(String[])",
            "com.example.overload.OverloadedApp.process(String)",
            "com.example.overload.OverloadedApp.process(String,int)");

    // Verify the signatures are distinct
    MethodSignature processOneArg =
        parsedFile.methods().stream()
            .filter(m -> m.qualifiedName().equals("com.example.overload.OverloadedApp.process(String)"))
            .findFirst()
            .orElseThrow();

    MethodSignature processTwoArgs =
        parsedFile.methods().stream()
            .filter(m -> m.qualifiedName().equals("com.example.overload.OverloadedApp.process(String,int)"))
            .findFirst()
            .orElseThrow();

    assertThat(processOneArg.parameters()).containsExactly("String");
    assertThat(processTwoArgs.parameters()).containsExactly("String", "int");

    // Both should be reachable from main (limitation: we can't distinguish which one is called)
    ReachabilityResult result1 =
        parser.findReachability(List.of(parsedFile), new Location(testFile, 11));
    ReachabilityResult result2 =
        parser.findReachability(List.of(parsedFile), new Location(testFile, 15));

    assertThat(result1.isReachable()).isTrue();
    assertThat(result2.isReachable()).isTrue();
  }

  @Test
  void testCallGraphVisualization() {
    // Given: A sample Java file
    Path testFile =
        Paths.get("src/test/resources/SampleApp.java").toAbsolutePath().normalize();
    JavaLanguageParser parser = new JavaLanguageParser();
    ParsedFile parsedFile = parser.parse(testFile);

    // Build call graph
    CallGraph graph = parser.buildCallGraph(List.of(parsedFile));

    // When: Export to different formats
    String dotFormat = graph.toDot();
    String mermaidFormat = graph.toMermaid();
    String textFormat = graph.toText();

    // Then: Verify formats contain expected content
    System.out.println("\n=== DOT (Graphviz) Format ===");
    System.out.println(dotFormat);
    assertThat(dotFormat)
        .contains("digraph CallGraph")
        .contains("main")
        .contains("processRequest")
        .contains("->");

    System.out.println("\n=== Mermaid Format ===");
    System.out.println(mermaidFormat);
    assertThat(mermaidFormat)
        .contains("graph LR")
        .contains("main")
        .contains("-->");

    System.out.println("\n=== Text Format ===");
    System.out.println(textFormat);
    assertThat(textFormat)
        .contains("Call Graph:")
        .contains("main")
        .contains("-> ");
  }
}

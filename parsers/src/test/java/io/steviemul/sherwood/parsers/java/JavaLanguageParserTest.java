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
    assertThat(result.confidence()).isEqualTo(1.0); // Reachable from entry point
    assertThat(result.allPaths()).isNotEmpty(); // Should have at least one path

    // The path should include: main -> processRequest -> executeLogic -> performCalculation
    assertThat(result.path())
        .hasSize(4)
        .extracting(node -> node.method().name())
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
    assertThat(unreachableResult.confidence()).isEqualTo(0.0); // Not reachable
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
  void testConfidenceScoring() {
    // Test case for confidence scoring:
    // - Confidence 1.0: Reachable from entry point (main)
    // - Confidence 0.5: Reachable from non-entry method
    // - Confidence 0.0: Not reachable

    Path testFile =
        Paths.get("src/test/resources/InternalApp.java").toAbsolutePath().normalize();
    JavaLanguageParser parser = new JavaLanguageParser();
    ParsedFile parsedFile = parser.parse(testFile);

    // Case 1: Method reachable from main (entry point) - confidence 1.0
    Location publicLocation = new Location(testFile, 10); // processPublic method
    ReachabilityResult publicResult = parser.findReachability(List.of(parsedFile), publicLocation);

    assertThat(publicResult.isReachable()).isTrue();
    assertThat(publicResult.confidence()).isEqualTo(1.0);
    assertThat(publicResult.entryPoint().name()).isEqualTo("main");
    System.out.println("\nPublic method (confidence 1.0): " + publicResult.path().stream()
        .map(node -> node.method().name())
        .toList());

    // Case 2: Method reachable but NOT from entry point - confidence 0.5
    Location internalLocation = new Location(testFile, 15); // internalHelper method
    ReachabilityResult internalResult =
        parser.findReachability(List.of(parsedFile), internalLocation);

    assertThat(internalResult.isReachable()).isTrue();
    assertThat(internalResult.confidence()).isEqualTo(0.5);
    assertThat(internalResult.entryPoint().name()).isEqualTo("isolatedMethod");
    System.out.println("\nInternal method (confidence 0.5): " + internalResult.path().stream()
        .map(node -> node.method().name())
        .toList());

    // Case 3: Method that calls others but is itself not reachable - confidence 0.0
    Location isolatedLocation = new Location(testFile, 20); // isolatedMethod itself
    ReachabilityResult isolatedResult =
        parser.findReachability(List.of(parsedFile), isolatedLocation);

    assertThat(isolatedResult.isReachable()).isFalse();
    assertThat(isolatedResult.confidence()).isEqualTo(0.0);
    System.out.println("\nIsolated method (confidence 0.0): Not reachable");
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

  @Test
  void testSourceCodeCapture() {
    // Given: A sample Java file
    Path testFile =
        Paths.get("src/test/resources/SampleApp.java").toAbsolutePath().normalize();
    JavaLanguageParser parser = new JavaLanguageParser();
    ParsedFile parsedFile = parser.parse(testFile);

    // When: Parse the file
    List<MethodSignature> methods = parsedFile.methods();

    // Then: Methods should have source code captured
    assertThat(methods).isNotEmpty();
    
    MethodSignature mainMethod = methods.stream()
        .filter(m -> m.name().equals("main"))
        .findFirst()
        .orElseThrow();

    assertThat(mainMethod.sourceCode())
        .isNotBlank()
        .contains("public static void main")
        .contains("processRequest");

    System.out.println("\n=== Captured Method Source ===");
    System.out.println("Method: " + mainMethod.name());
    System.out.println("Lines: " + mainMethod.startLine() + "-" + mainMethod.endLine());
    System.out.println("Source:\n" + mainMethod.sourceCode());

    // When: Analyze reachability for a specific line
    Location targetLocation = new Location(testFile, 16); // Line in executeLogic
    ReachabilityResult result = parser.findReachability(List.of(parsedFile), targetLocation);

    // Then: Target snippet should be captured with context
    assertThat(result.isReachable()).isTrue();
    assertThat(result.targetSnippet())
        .isNotBlank()
        .contains("16:"); // Should include line number

    System.out.println("\n=== Target Snippet (with context) ===");
    System.out.println(result.targetSnippet());
  }

  @Test
  void testInitializerBlocks() {
    // Given: A file with static/instance initializers
    Path testFile =
        Paths.get("src/test/resources/InitializerApp.java").toAbsolutePath().normalize();
    JavaLanguageParser parser = new JavaLanguageParser();
    ParsedFile parsedFile = parser.parse(testFile);

    System.out.println("\n=== Testing Initializer Blocks ===");

    // Case 1: Line in static field initializer (line 5)
    Location staticFieldLocation = new Location(testFile, 5);
    ReachabilityResult staticFieldResult =
        parser.findReachability(List.of(parsedFile), staticFieldLocation);

    System.out.println("\nStatic field initializer (line 5): " + staticFieldResult.isReachable());
    System.out.println("Confidence: " + staticFieldResult.confidence());
    
    // Case 2: Line in static block (line 13)
    Location staticBlockLocation = new Location(testFile, 13);
    ReachabilityResult staticBlockResult =
        parser.findReachability(List.of(parsedFile), staticBlockLocation);

    System.out.println("\nStatic block (line 13): " + staticBlockResult.isReachable());
    System.out.println("Confidence: " + staticBlockResult.confidence());

    // Case 3: Line in instance field initializer (line 8)
    Location instanceFieldLocation = new Location(testFile, 8);
    ReachabilityResult instanceFieldResult =
        parser.findReachability(List.of(parsedFile), instanceFieldLocation);

    System.out.println("\nInstance field initializer (line 8): " + instanceFieldResult.isReachable());
    System.out.println("Confidence: " + instanceFieldResult.confidence());

    // Static initializers run at class load - confidence 1.0 (entry point level)
    assertThat(staticFieldResult.isReachable()).isTrue();
    assertThat(staticFieldResult.confidence()).isEqualTo(1.0);
    assertThat(staticFieldResult.entryPoint().name()).isEqualTo("<clinit>");

    assertThat(staticBlockResult.isReachable()).isTrue();
    assertThat(staticBlockResult.confidence()).isEqualTo(1.0);

    // Instance initializers run on construction - confidence 0.8 (high but not entry point)
    assertThat(instanceFieldResult.isReachable()).isTrue();
    assertThat(instanceFieldResult.confidence()).isEqualTo(0.8);
    assertThat(instanceFieldResult.entryPoint().name()).isEqualTo("<init>");
  }

  @Test
  void testAnnotationLines() {
    // Given: A file with annotated methods
    Path testFile =
        Paths.get("src/test/resources/AnnotatedMethodApp.java").toAbsolutePath().normalize();
    JavaLanguageParser parser = new JavaLanguageParser();
    ParsedFile parsedFile = parser.parse(testFile);

    System.out.println("\n=== Testing Annotation Line Handling ===");

    // Find the getUsers method
    MethodSignature getUsersMethod = parsedFile.methods().stream()
        .filter(m -> m.name().equals("getUsers"))
        .findFirst()
        .orElseThrow();

    System.out.println("\nMethod: " + getUsersMethod.name());
    System.out.println("Start line: " + getUsersMethod.startLine());
    System.out.println("End line: " + getUsersMethod.endLine());
    System.out.println("Annotations: " + getUsersMethod.annotations());

    // The method has annotations on lines 5-6, declaration on line 7
    // Start line should be 5 (first annotation), not 7
    assertThat(getUsersMethod.startLine()).isEqualTo(5);
    assertThat(getUsersMethod.endLine()).isEqualTo(9);
    assertThat(getUsersMethod.annotations()).hasSize(2);

    // Case: SARIF result points to annotation line (line 5)
    Location annotationLocation = new Location(testFile, 5);
    ReachabilityResult annotationResult =
        parser.findReachability(List.of(parsedFile), annotationLocation);

    System.out.println("\nAnnotation line (line 5): " + annotationResult.isReachable());
    System.out.println("Confidence: " + annotationResult.confidence());

    // Should find the method since annotations are now included in line range
    assertThat(annotationResult.isReachable()).isTrue();
    assertThat(annotationResult.confidence()).isEqualTo(1.0); // Entry point (has @RequestMapping)

    // Case: SARIF result points to second annotation line (line 6)
    Location secondAnnotationLocation = new Location(testFile, 6);
    ReachabilityResult secondAnnotationResult =
        parser.findReachability(List.of(parsedFile), secondAnnotationLocation);

    assertThat(secondAnnotationResult.isReachable()).isTrue();
    assertThat(secondAnnotationResult.confidence()).isEqualTo(1.0);
  }

  @Test
  void testReachabilityResultVisualization() {
    // Given: A sample Java file with reachable methods
    Path testFile =
        Paths.get("src/test/resources/SampleApp.java").toAbsolutePath().normalize();
    JavaLanguageParser parser = new JavaLanguageParser();
    ParsedFile parsedFile = parser.parse(testFile);

    // Target: performCalculation method (deep in call chain)
    Location targetLocation = new Location(testFile, 22);
    ReachabilityResult result = parser.findReachability(List.of(parsedFile), targetLocation);

    System.out.println("\n=== Reachability Result Visualization ===");

    // Verify it's reachable
    assertThat(result.isReachable()).isTrue();
    assertThat(result.allPaths()).isNotEmpty();

    // Test DOT format
    String dotFormat = result.toDot();
    System.out.println("\n--- DOT Format ---");
    System.out.println(dotFormat);

    assertThat(dotFormat)
        .contains("digraph ReachabilityGraph")
        .contains("main")
        .contains("performCalculation")
        .contains("->");

    // Test Mermaid format
    String mermaidFormat = result.toMermaid();
    System.out.println("\n--- Mermaid Format ---");
    System.out.println(mermaidFormat);

    assertThat(mermaidFormat)
        .contains("graph TD")
        .contains("main")
        .contains("performCalculation")
        .contains("-->");

    // Test Text format
    String textFormat = result.toText();
    System.out.println("\n--- Text Format ---");
    System.out.println(textFormat);

    assertThat(textFormat)
        .contains("Reachability Paths:")
        .contains("Confidence: 1.0")
        .contains("main")
        .contains("performCalculation")
        .contains("Path 1:");

    // Test unreachable case
    Location unreachableLocation = new Location(testFile, 30); // unreachableMethod
    ReachabilityResult unreachableResult =
        parser.findReachability(List.of(parsedFile), unreachableLocation);

    assertThat(unreachableResult.isReachable()).isFalse();
    assertThat(unreachableResult.toDot()).contains("No reachable paths");
    assertThat(unreachableResult.toMermaid()).contains("No reachable paths");
    assertThat(unreachableResult.toText()).contains("No reachable paths found");
  }
}

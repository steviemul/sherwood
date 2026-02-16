package io.steviemul.sherwood.cli.context;

import io.steviemul.sherwood.sarif.ArtifactContent;
import io.steviemul.sherwood.sarif.PhysicalLocation;
import io.steviemul.sherwood.sarif.Region;
import io.steviemul.sherwood.sarif.Result;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class CodeContextResolver {

  private final Path sourceCodeRoot;

  public void addCodeSnippetIfRequired(Result result) {

    try {
      PhysicalLocation physicalLocation = result.getLocations().getFirst().getPhysicalLocation();
      Region region = physicalLocation.getRegion();

      ArtifactContent snippet =
          Optional.ofNullable(region.getSnippet()).orElse(new ArtifactContent());

      if (snippet.getText() == null || snippet.getText().isEmpty()) {
        String code = getSourceCode(result);

        if (code != null && !code.trim().isEmpty()) {
          snippet.setText(code);
        }
      }

      region.setSnippet(snippet);
    } catch (Exception e) {
      log.warn("Unable to add code snippet", e);
    }
  }

  private String getSourceCode(Result result) {

    try {
      SourceCodeLocation location = getSourceCodeLocation(result);

      if (location == null) return null;

      Path sourceCodePath = Path.of(sourceCodeRoot.toString(), location.path);

      if (Files.exists(sourceCodePath)) {
        List<String> lines = Files.readAllLines(sourceCodePath, StandardCharsets.UTF_8);

        log.info(
            "Adding source code context for file at line : [{}, {}]",
            location.path,
            location.startLine);

        // lines is 0 indexed, but line numbers in code start at 1, so startLine -1 for correct
        // array index
        return getLines(lines, (int) location.startLine - 1, (int) location.endLine);
      } else {
        log.error("Source code not found: {}", sourceCodePath);
      }
    } catch (Exception e) {
      log.error("Error reading source code file: {}", sourceCodeRoot, e);
    }

    return null;
  }

  private String getLines(List<String> lines, int startLine, int endLine) {
    return String.join("\n", lines.subList(startLine, endLine));
  }

  private SourceCodeLocation getSourceCodeLocation(Result result) {

    try {
      PhysicalLocation physicalLocation = result.getLocations().getFirst().getPhysicalLocation();

      String path = physicalLocation.getArtifactLocation().getUri();

      return new SourceCodeLocation(
          path,
          physicalLocation.getRegion().getStartLine(),
          physicalLocation.getRegion().getEndLine());
    } catch (Exception e) {
      log.warn("Error retrieving source code", e);
    }

    return null;
  }

  private record SourceCodeLocation(String path, long startLine, long endLine) {}
}

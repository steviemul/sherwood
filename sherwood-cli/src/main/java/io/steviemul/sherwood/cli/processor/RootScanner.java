package io.steviemul.sherwood.cli.processor;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RootScanner {

  private final Path root;

  public List<Path> getAllMatchingFiles() {

    List<PathMatcher> matchers = List.of(FileSystems.getDefault().getPathMatcher("glob:**/*.java"));

    try (Stream<Path> paths = Files.walk(root)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(p -> matchers.stream().anyMatch(m -> m.matches(p)))
          .toList();
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }
}

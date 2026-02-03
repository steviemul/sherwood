package io.steviemul.sherwood.cli.processor;

import io.steviemul.sherwood.sarif.SarifSchema210;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReachabilityAnalyser {

  private final List<Path> codeFiles;
  private final SarifSchema210 sarif;
}

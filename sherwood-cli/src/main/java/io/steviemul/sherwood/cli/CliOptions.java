package io.steviemul.sherwood.cli;

import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Option;

@Getter
@Setter
public class CliOptions {

  @Option(
      names = {"-r", "--root"},
      description = "Root directory where source code is located",
      required = true)
  private Path root;

  @Option(
      names = {"-s", "--sarif"},
      description = "Location of sarif file containing analysis results",
      required = true)
  private Path sarif;

  @Option(
      names = {"-o", "--output"},
      description =
          "Output location to save the modified SARIF file, defaults to 'sherwood-output.sarif' ",
      defaultValue = "sherwood-output.sarif")
  private Path output;
}

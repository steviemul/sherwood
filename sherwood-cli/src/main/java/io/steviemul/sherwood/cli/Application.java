package io.steviemul.sherwood.cli;

import io.steviemul.sherwood.cli.options.CliOptions;
import io.steviemul.sherwood.cli.output.CliOutput;
import io.steviemul.sherwood.cli.processor.Analyser;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(
    name = "sherwood",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "Sherwood CLI application")
public class Application implements Callable<Integer> {

  @Mixin private CliOptions options;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Application()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    CliOutput.printBanner();

    Analyser analyser =
        Analyser.builder()
            .sourceCodeRoot(options.getRoot())
            .sarifPath(options.getSarif())
            .outputPath(options.getOutput())
            .build();

    analyser.analyse();

    return 0;
  }
}

package io.steviemul.sherwood.cli;

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
  public Integer call() throws Exception {
    System.out.println("Sherwood CLI running...");

    if (options.getRoot() != null) {
      System.out.println("Root directory: " + options.getRoot());
    }

    return 0;
  }
}

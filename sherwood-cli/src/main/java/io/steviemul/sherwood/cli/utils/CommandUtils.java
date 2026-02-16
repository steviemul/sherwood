package io.steviemul.sherwood.cli.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Optional;

public class CommandUtils {

  public static Optional<String> runCommand(File workingDir, String... command) {

    try {
      ProcessBuilder pb = new ProcessBuilder(command);

      pb.directory(workingDir);
      pb.redirectErrorStream(true); // merge stderr into stdout

      Process process = pb.start();

      // Capture output
      StringBuilder output = new StringBuilder();
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()))) {

        String line;

        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }

      int exitCode = process.waitFor();

      if (exitCode == 0) {
        return Optional.of(output.toString());
      } else {
        return Optional.empty();
      }
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}

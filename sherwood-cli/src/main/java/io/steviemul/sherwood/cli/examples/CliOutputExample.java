package io.steviemul.sherwood.cli.examples;

import io.steviemul.sherwood.cli.output.CliOutput;
import java.util.LinkedHashMap;
import java.util.Map;
import me.tongfei.progressbar.ProgressBar;

/** Example demonstrating the various CLI output capabilities. */
public class CliOutputExample {

  public static void main(String[] args) throws InterruptedException {
    // Print banner
    CliOutput.printBanner();

    // Task messages
    CliOutput.info("Starting analysis...");

    // Simulate reading files
    System.out.print("Reading files from codebase... ");
    Thread.sleep(1000);
    CliOutput.taskComplete("Reading files from codebase");

    // Progress bar example
    int totalResults = 100;
    try (ProgressBar pb = CliOutput.createProgressBar("Processing SARIF results", totalResults)) {
      for (int i = 0; i < totalResults; i++) {
        Thread.sleep(30);
        pb.step();
        pb.setExtraMessage("Result " + (i + 1) + "/" + totalResults);
      }
    }

    // More task completions
    CliOutput.taskComplete("Analyzed security findings");
    CliOutput.taskComplete("Generated recommendations");

    // Warning example
    CliOutput.warning("Found 3 high-severity issues");

    // Summary table
    Map<String, String> summary = new LinkedHashMap<>();
    summary.put("Files scanned", "247");
    summary.put("SARIF results processed", "100");
    summary.put("Critical issues", "5");
    summary.put("High severity", "12");
    summary.put("Medium severity", "23");
    summary.put("Low severity", "60");
    summary.put("Analysis duration", "2.3s");

    CliOutput.printSummaryTable("Analysis Summary", summary);

    // Final message
    CliOutput.taskComplete("Analysis complete");
  }
}

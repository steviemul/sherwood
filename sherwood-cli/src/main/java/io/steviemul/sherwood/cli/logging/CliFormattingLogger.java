package io.steviemul.sherwood.cli.logging;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import java.util.Map;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.fusesource.jansi.Ansi;

/** Utility class for formatted CLI output with colors, progress bars, and tables. */
public class CliFormattingLogger {

  private static final String CHECKMARK = "✓";
  private static final String CROSS = "✗";

  /** Print a task status with a checkmark when complete. */
  public static void taskComplete(String message) {
    System.out.println(Ansi.ansi().fgGreen().a(CHECKMARK).reset().a(" " + message));
  }

  /** Print a task failure with a cross. */
  public static void taskFailed(String message) {
    System.out.println(Ansi.ansi().fgRed().a(CROSS).reset().a(" " + message));
  }

  /** Print an info message. */
  public static void info(String message) {
    System.out.println(Ansi.ansi().fgCyan().a("ℹ").reset().a(" " + message));
  }

  /** Print a warning message. */
  public static void warning(String message) {
    System.out.println(Ansi.ansi().fgYellow().a("⚠").reset().a(" " + message));
  }

  /** Print an error message. */
  public static void error(String message) {
    System.err.println(Ansi.ansi().fgRed().a("✗").reset().a(" " + message));
  }

  /** Create a progress bar for tracking task progress. */
  public static ProgressBar createProgressBar(String taskName, long total) {
    return new ProgressBarBuilder()
        .setTaskName(taskName)
        .setInitialMax(total)
        .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
        .build();
  }

  /** Print a summary table with key-value pairs. */
  public static void printSummaryTable(String title, Map<String, String> data) {
    System.out.println("\n" + Ansi.ansi().bold().a(title).reset());
    System.out.println(Ansi.ansi().a("═".repeat(title.length())).reset());

    AsciiTable table = new AsciiTable();
    table.setTextAlignment(TextAlignment.LEFT);

    data.forEach(
        (key, value) -> {
          table.addRule();
          table.addRow(key, value);
        });
    table.addRule();

    System.out.println(table.render());
  }

  /** Print a banner with the Sherwood ASCII art. */
  public static void printBanner() {
    System.out.println(
        Ansi.ansi()
            .fgBrightGreen()
            .a(
                """
           🏹
            \\\\
             \\\\        ███████╗██╗  ██╗███████╗██████╗ ██╗    ██╗ ██████╗  ██████╗ ██████╗
              \\\\       ██╔════╝██║  ██║██╔════╝██╔══██╗██║    ██║██╔═══██╗██╔═══██╗██╔══██╗
               ==>     ███████╗███████║█████╗  ██████╔╝██║ █╗ ██║██║   ██║██║   ██║██║  ██║
               //      ╚════██║██╔══██║██╔══╝  ██╔══██╗██║███╗██║██║   ██║██║   ██║██║  ██║
              //       ███████║██║  ██║███████╗██║  ██║╚███╔███╔╝╚██████╔╝╚██████╔╝██████╔╝
             //        ╚══════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝ ╚══╝╚══╝  ╚═════╝  ╚═════╝ ╚═════╝
        """)
            .reset()
            .a("\n")
            .fgCyan()
            .a("            Taking from noisy SARIF • Giving actionable security insight\n")
            .reset());
  }
}

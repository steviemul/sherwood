package io.steviemul.sherwood.cli.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;

public class CliLogbackAppender extends AppenderBase<ILoggingEvent> {

  @Override
  protected void append(ILoggingEvent event) {

    if (event == null) {
      return;
    }

    String message = event.getFormattedMessage();
    Level level = event.getLevel();

    if (level == Level.ERROR) {
      CliFormattingLogger.error(message);
      printThrowableIfPresent(event);
      return;
    }

    if (level == Level.WARN) {
      CliFormattingLogger.warning(message);
      printThrowableIfPresent(event);
      return;
    }

    if (level == Level.INFO) {
      CliFormattingLogger.info(message);
      printThrowableIfPresent(event);
    }
  }

  private static void printThrowableIfPresent(ILoggingEvent event) {
    if (event.getThrowableProxy() == null) {
      return;
    }
    String stack = ThrowableProxyUtil.asString(event.getThrowableProxy());
    // keep stack traces readable; send to stderr
    System.err.print(stack);
  }
}

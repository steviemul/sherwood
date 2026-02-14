package io.steviemul.sherwood.server.exception;

public class SarifProcessingException extends RuntimeException {

  public SarifProcessingException(String message) {
    super(message);
  }

  public SarifProcessingException(String message, Exception cause) {
    super(message, cause);
  }
}

package io.steviemul.sherwood.cli.processor;

import io.steviemul.sherwood.cli.http.SherwoodApiClient;
import io.steviemul.sherwood.cli.logging.CliFormattingLogger;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class SarifUploader {

  private final Path sarifPath;
  private final String serverUrl;

  public void uploadSarif() {

    try (SherwoodApiClient apiClient = new SherwoodApiClient(serverUrl)) {

      boolean success = apiClient.uploadSarif(sarifPath.toFile());

      if (success) {
        CliFormattingLogger.taskComplete("Sarif uploaded successfully");
      } else {
        CliFormattingLogger.taskFailed("Unable to upload sarif");
      }
    } catch (Exception e) {
      log.error("Error upload sarif " + e.getMessage());
    }
  }
}

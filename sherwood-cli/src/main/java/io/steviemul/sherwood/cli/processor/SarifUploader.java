package io.steviemul.sherwood.cli.processor;

import io.steviemul.sherwood.cli.http.SherwoodApiClient;
import io.steviemul.sherwood.cli.logging.Logger;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SarifUploader {

  private final Path sarifPath;
  private final String serverUrl;

  public void uploadSarif() {

    try (SherwoodApiClient apiClient = new SherwoodApiClient(serverUrl)) {

      boolean success = apiClient.uploadSarif(sarifPath.toFile());

      if (success) {
        Logger.taskComplete("Sarif uploaded successfully");
      } else {
        Logger.taskFailed("Unable to upload sarif");
      }
    } catch (Exception e) {
      Logger.error("Error upload sarif " + e.getMessage());
    }
  }
}

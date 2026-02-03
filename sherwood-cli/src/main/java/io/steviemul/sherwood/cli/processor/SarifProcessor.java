package io.steviemul.sherwood.cli.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.steviemul.sherwood.cli.logging.Logger;
import io.steviemul.sherwood.sarif.SarifSchema210;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SarifProcessor {

  private final Path sarifPath;

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public SarifSchema210 readSarif() {

    try {
      return objectMapper.readValue(sarifPath.toFile(), SarifSchema210.class);
    } catch (Exception e) {

    }

    return null;
  }

  public void writeSarif(SarifSchema210 sarif, Path outputPath) {

    try (OutputStream out = new FileOutputStream(outputPath.toFile())) {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(out, sarif);
    } catch (Exception e) {
      Logger.error("Error outputting results " + e.getMessage());
    }
  }
}

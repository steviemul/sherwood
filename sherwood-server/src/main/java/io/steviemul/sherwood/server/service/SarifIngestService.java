package io.steviemul.sherwood.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.steviemul.sherwood.sarif.Result;
import io.steviemul.sherwood.sarif.SarifSchema210;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SarifIngestService {

  private final StorageService storageService;
  private final Logger jobLogger = new JobRunrDashboardLogger(log);
  private final ObjectMapper objectMapper;

  public void ingestSarif(String storageKey) {
    jobLogger.info("Ingesting sarif with key {}", storageKey);

    SarifSchema210 sarif = readSarif(storageKey);

    processSarif(sarif);

    jobLogger.info("Ingest complete for key {}", storageKey);
  }

  private void processSarif(SarifSchema210 sarif) {

    List<Result> results = sarif.getRuns().getFirst().getResults();

    results.forEach(this::processResult);
  }

  private void processResult(Result result) {
    jobLogger.info("Processing result {}", result.getRuleId());
  }

  private SarifSchema210 readSarif(String storageKey) {

    try (InputStream is = storageService.getObject(storageKey)) {
      return objectMapper.readValue(is, SarifSchema210.class);
    } catch (IOException e) {
      throw new RuntimeException("Error processing sarif", e);
    }
  }
}

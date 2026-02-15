package io.steviemul.sherwood.server.service.sarif;

import io.steviemul.sherwood.sarif.SarifSchema210;
import java.util.UUID;
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
  private final SarifService sarifService;

  public void ingestSarif(String storageKey, UUID sarifId) {
    jobLogger.info("Ingesting sarif with key {}", storageKey);

    SarifSchema210 sarifFile = storageService.readSarif(storageKey);

    processSarif(sarifFile, sarifId);

    jobLogger.info("Ingest complete for key {}", storageKey);
  }

  private void processSarif(SarifSchema210 sarif, UUID sarifId) {
    sarifService.processSarif(sarif, sarifId);
  }
}

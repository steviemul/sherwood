package io.steviemul.sherwood.server.service.jobs;

import io.steviemul.sherwood.server.service.rules.SarifRulesIngestService;
import io.steviemul.sherwood.server.service.sarif.SarifIngestService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class JobService {

  private final JobScheduler jobScheduler;
  private final SarifIngestService sarifIngestService;
  private final SarifRulesIngestService sarifRulesIngestService;

  public UUID submitSarifIngestJob(String storageKey, UUID sarifId) {
    jobScheduler.enqueue(sarifId, () -> sarifIngestService.ingestSarif(storageKey, sarifId));

    return sarifId;
  }

  public UUID submitSarifRulesIngestJob(String storageKey) {

    UUID jobId = UUID.randomUUID();

    jobScheduler.enqueue(jobId, () -> sarifRulesIngestService.ingestSarifRules(storageKey));

    return jobId;
  }
}

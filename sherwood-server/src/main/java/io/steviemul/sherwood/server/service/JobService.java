package io.steviemul.sherwood.server.service;

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

  public UUID submitSarifIngestJob(String storageKey) {
    UUID jobId = UUID.randomUUID();

    jobScheduler.enqueue(jobId, () -> sarifIngestService.ingestSarif(storageKey));

    return jobId;
  }
}

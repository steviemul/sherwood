package io.steviemul.sherwood.server.controller;

import static io.steviemul.sherwood.server.constant.Routes.SARIFS_ROUTE;
import static io.steviemul.sherwood.server.constant.Routes.STATUS_ROUTE;

import io.steviemul.sherwood.server.entity.sarif.Sarif;
import io.steviemul.sherwood.server.response.SarifResponse;
import io.steviemul.sherwood.server.service.JobService;
import io.steviemul.sherwood.server.service.SarifService;
import io.steviemul.sherwood.server.service.StorageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SherwoodController {

  private final SarifService sarifService;
  private final StorageService storageService;
  private final JobService jobService;

  @GetMapping(STATUS_ROUTE)
  public ResponseEntity<String> getStatus() {

    return ResponseEntity.ok("RUNNING");
  }

  @PostMapping(SARIFS_ROUTE)
  public ResponseEntity<String> uploadSarif(@RequestPart("sarif") MultipartFile sarifFile) {
    try {
      String key = storageService.uploadSarif(sarifFile);

      log.info("Sarif file saved to {}", key);

      Sarif sarif = sarifService.createSarif(sarifFile.getOriginalFilename(), key);

      log.info("Sarif info saved with id {}", sarif.getId());

      UUID jobId = jobService.submitSarifIngestJob(key, sarif.getId());

      log.info("Sarif ingest job submitted with job id {}", jobId);

      return ResponseEntity.accepted().build();
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("Unable to save sarif : " + e.getMessage());
    }
  }

  @GetMapping(SARIFS_ROUTE + "/{id}")
  public ResponseEntity<SarifResponse> getSarif(@PathVariable("id") UUID id) {
    SarifResponse sarifResponse = sarifService.getSarifResponseById(id);

    return ResponseEntity.ok(sarifResponse);
  }
}

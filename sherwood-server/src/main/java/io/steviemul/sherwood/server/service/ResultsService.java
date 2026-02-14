package io.steviemul.sherwood.server.service;

import io.steviemul.sherwood.sarif.Result;
import io.steviemul.sherwood.sarif.SarifSchema210;
import io.steviemul.sherwood.server.entity.sarif.Sarif;
import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import io.steviemul.sherwood.server.repository.ResultsRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class ResultsService {

  private final ResultsRepository resultsRepository;

  public void processResults(SarifSchema210 sarifFile, Sarif sarif) {

    List<SarifResult> results =
        sarifFile.getRuns().getFirst().getResults().stream()
            .map(result -> toSarifResultEntity(result, sarif))
            .toList();

    resultsRepository.saveAll(results);
  }

  private SarifResult toSarifResultEntity(Result result, Sarif sarif) {

    String fingerPrint =
        result.getFingerprints().getAdditionalProperties().getOrDefault("test", "test");

    return SarifResult.builder()
        .sarif(sarif)
        .fingerprint(fingerPrint)
        .ruleId(result.getRuleId())
        .description(result.getMessage().getText())
        .location(getLocation(result))
        .lineNumber(getLineNumber(result))
        .build();
  }

  private String getLocation(Result result) {
    return result.getLocations().getFirst().getPhysicalLocation().getArtifactLocation().getUri();
  }

  private long getLineNumber(Result result) {
    return result.getLocations().getFirst().getPhysicalLocation().getRegion().getStartLine();
  }

  public List<SarifResult> getResultsBySarifId(UUID sarifId) {
    return resultsRepository.findBySarifId(sarifId);
  }
}

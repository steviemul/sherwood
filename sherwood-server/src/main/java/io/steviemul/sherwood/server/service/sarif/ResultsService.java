package io.steviemul.sherwood.server.service.sarif;

import static io.steviemul.sherwood.server.utils.ResultHelper.getAnalysis;
import static io.steviemul.sherwood.server.utils.ResultHelper.getFingerprint;
import static io.steviemul.sherwood.server.utils.ResultHelper.getLineNumber;
import static io.steviemul.sherwood.server.utils.ResultHelper.getLocation;
import static io.steviemul.sherwood.server.utils.ResultHelper.getSnippet;

import io.steviemul.sherwood.sarif.Result;
import io.steviemul.sherwood.sarif.SarifSchema210;
import io.steviemul.sherwood.server.entity.sarif.AnalysisPath;
import io.steviemul.sherwood.server.entity.sarif.ResultAnalysis;
import io.steviemul.sherwood.server.entity.sarif.ResultPathFingerprint;
import io.steviemul.sherwood.server.entity.sarif.Sarif;
import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import io.steviemul.sherwood.server.repository.ResultsRepository;
import java.util.List;
import java.util.Optional;
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

  public List<SarifResult> getResultsBySarifId(UUID sarifId) {
    return resultsRepository.findBySarifId(sarifId);
  }

  public Optional<SarifResult> getResultById(UUID resultId) {
    return resultsRepository.findById(resultId);
  }

  private SarifResult toSarifResultEntity(Result result, Sarif sarif) {

    ResultAnalysis analysis = getAnalysis(result);

    SarifResult sarifResult =
        SarifResult.builder()
            .sarif(sarif)
            .fingerprint(getFingerprint(result))
            .ruleId(result.getRuleId())
            .description(result.getMessage().getText())
            .location(getLocation(result))
            .lineNumber(getLineNumber(result))
            .snippet(getSnippet(result))
            .confidence(analysis.confidence())
            .reachable(analysis.reachable())
            .graph(analysis.graph())
            .build();

    // Populate path fingerprints if path exists
    List<AnalysisPath> path = analysis.path();

    if (path != null && !path.isEmpty()) {
      for (int i = 0; i < path.size(); i++) {
        AnalysisPath analysisPath = path.get(i);
        String fingerprint = analysisPath.getFingerprint();

        ResultPathFingerprint pathFingerprint =
            ResultPathFingerprint.builder().fingerprint(fingerprint).fingerprintOrder(i).build();

        sarifResult.getPathFingerprints().add(pathFingerprint);
      }
    }

    return sarifResult;
  }
}

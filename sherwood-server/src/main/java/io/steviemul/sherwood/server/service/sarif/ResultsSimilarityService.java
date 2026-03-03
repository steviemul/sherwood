package io.steviemul.sherwood.server.service.sarif;

import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import io.steviemul.sherwood.server.repository.ResultsRepository;
import io.steviemul.sherwood.server.response.SarifResultSimilarityResponse;
import io.steviemul.sherwood.server.scoring.ResultSimilarityScore;
import io.steviemul.sherwood.server.scoring.ResultsScoringService;
import io.steviemul.sherwood.server.scoring.SimilarityScore;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResultsSimilarityService {

  private final ResultsRepository resultsRepository;
  private final ResultsScoringService resultsScoringService;

  private static final int LINE_NUMBER_THRESHOLD = 5;
  private static final double SIMILARITY_THRESHOLD = 0.0;

  public List<SarifResultSimilarityResponse> findSimilarResults(UUID sarifId, UUID resultId) {

    SarifResult result = getResult(sarifId, resultId);

    String repository = result.getSarif().getRepository();
    String location = result.getLocation();

    return resultsRepository
        .findBySarifRepositoryAndLocationContainingIgnoreCase(repository, location)
        .stream()
        .filter(c -> doIdsDiffer(result, c))
        .filter(c -> isWithinLineNumberThreshold(result, c))
        .map(c -> toSimilarityResponse(result, c))
        .filter(r -> r.similarity().totalScore() > SIMILARITY_THRESHOLD)
        .sorted(
            Comparator.comparingDouble(
                    (SarifResultSimilarityResponse r) -> r.similarity().totalScore())
                .reversed())
        .toList();
  }

  private boolean doIdsDiffer(SarifResult resultA, SarifResult resultB) {
    return !resultA.getId().equals(resultB.getId());
  }

  private boolean isWithinLineNumberThreshold(SarifResult resultA, SarifResult resultB) {

    return (resultB.getLineNumber() >= (resultA.getLineNumber() - LINE_NUMBER_THRESHOLD))
        && (resultB.getLineNumber() <= (resultA.getLineNumber() + LINE_NUMBER_THRESHOLD));
  }

  private SarifResultSimilarityResponse toSimilarityResponse(
      SarifResult result, SarifResult candidate) {

    ResultSimilarityScore similarityScore =
        resultsScoringService.getSimilarityScore(result, candidate);

    if (result.getFingerprint().equals(candidate.getFingerprint())) {
      return getFingerprintMatchesSimilarityResponse(candidate);
    }

    return new SarifResultSimilarityResponse(
        candidate.getId(),
        candidate.getSarif().getId(),
        candidate.getLocation(),
        candidate.getLineNumber(),
        candidate.getRuleId(),
        candidate.getDescription(),
        candidate.getSnippet(),
        candidate.getSarif().getVendor(),
        similarityScore);
  }

  private SarifResultSimilarityResponse getFingerprintMatchesSimilarityResponse(
      SarifResult candidate) {

    SimilarityScore score = new SimilarityScore("Fingerprints Match", 1.0, 1.0, true, "");

    return new SarifResultSimilarityResponse(
        candidate.getId(),
        candidate.getSarif().getId(),
        candidate.getLocation(),
        candidate.getLineNumber(),
        candidate.getRuleId(),
        candidate.getDescription(),
        candidate.getSnippet(),
        candidate.getSarif().getVendor(),
        new ResultSimilarityScore(1.0, 1.0, List.of(score)));
  }

  private SarifResult getResult(UUID sarifId, UUID resultId) {
    return resultsRepository
        .findBySarifIdAndId(sarifId, resultId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Result not found with id: " + resultId));
  }
}

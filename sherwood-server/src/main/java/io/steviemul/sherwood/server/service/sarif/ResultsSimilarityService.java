package io.steviemul.sherwood.server.service.sarif;

import static io.steviemul.sherwood.server.utils.SimilarityUtils.getCosineSimilarity;

import io.steviemul.sherwood.server.entity.rule.OllamaRule;
import io.steviemul.sherwood.server.entity.sarif.ResultPathFingerprint;
import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import io.steviemul.sherwood.server.repository.OllamaRuleRepository;
import io.steviemul.sherwood.server.repository.ResultsRepository;
import io.steviemul.sherwood.server.response.SarifResultSimilarityResponse;
import io.steviemul.sherwood.server.service.code.CodeSimilarityService;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResultsSimilarityService {

  private final ResultsRepository resultsRepository;
  private final OllamaRuleRepository ollamaRuleRepository;
  private final CodeSimilarityService codeSimilarityService;

  private static final int LINE_NUMBER_THRESHOLD = 5;
  private static final double SIMILARITY_THRESHOLD = 0.0;

  private static final double RULE_SIMILARITY_WEIGHT = 0.4;
  private static final double CODE_SIMILARITY_WEIGHT = 0.3;
  private static final double DISTANCE_WEIGHT = 0.4;
  private static final double PATH_SIMILARITY_WEIGHT = 0.3;

  private static final String REASON_TEMPLATE =
      """
      Location : %s matched (1.0)
      Line Distance : %s
      Rule Similarity : %s
      Code Similarity : %s
      Path Similarity : %s
    """;

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
        .filter(r -> r.similarity() > SIMILARITY_THRESHOLD)
        .sorted(Comparator.comparingDouble(SarifResultSimilarityResponse::similarity).reversed())
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

    if (result.getFingerprint().equals(candidate.getFingerprint())) {
      return getFingerprintMatchesSimilarityResponse(candidate);
    }

    OllamaRule resultRule = getRule(result.getRuleId());
    OllamaRule candidateRule = getRule(candidate.getRuleId());

    double ruleSimilarity =
        getCosineSimilarity(resultRule.getEmbedding(), candidateRule.getEmbedding());

    long distance = Math.abs(result.getLineNumber() - candidate.getLineNumber());

    double snippetSimilarity = getSnippetSimilarity(result, candidate);

    double pathSimilarity = getPathSimilarity(result, candidate);

    double similarity =
        calculateCombinedSimilarity(ruleSimilarity, distance, snippetSimilarity, pathSimilarity);

    String reason =
        REASON_TEMPLATE.formatted(
            result.getLocation(), distance, ruleSimilarity, snippetSimilarity, pathSimilarity);

    return new SarifResultSimilarityResponse(
        candidate.getId(),
        candidate.getSarif().getId(),
        candidate.getLocation(),
        candidate.getLineNumber(),
        candidate.getRuleId(),
        candidate.getDescription(),
        candidate.getSnippet(),
        similarity,
        reason,
        candidate.getSarif().getVendor());
  }

  private double getPathSimilarity(SarifResult resultA, SarifResult resultB) {

    List<ResultPathFingerprint> fingerprintsA = resultA.getPathFingerprints();
    List<ResultPathFingerprint> fingerprintsB = resultB.getPathFingerprints();

    // If either or both lists are empty, return 0.0
    if (fingerprintsA.isEmpty() || fingerprintsB.isEmpty()) {
      return 0.0;
    }

    // Count position-based matches
    int matches = 0;
    int minLength = Math.min(fingerprintsA.size(), fingerprintsB.size());

    for (int i = 0; i < minLength; i++) {
      String fpA = fingerprintsA.get(i).getFingerprint();
      String fpB = fingerprintsB.get(i).getFingerprint();

      boolean isMatch = fpA.equals(fpB);

      if (isMatch) {
        matches++;
      }
    }

    // Return matched count divided by length of longer list
    int maxLength = Math.max(fingerprintsA.size(), fingerprintsB.size());

    return (double) matches / maxLength;
  }

  private SarifResultSimilarityResponse getFingerprintMatchesSimilarityResponse(
      SarifResult candidate) {

    return new SarifResultSimilarityResponse(
        candidate.getId(),
        candidate.getSarif().getId(),
        candidate.getLocation(),
        candidate.getLineNumber(),
        candidate.getRuleId(),
        candidate.getDescription(),
        candidate.getSnippet(),
        1.0,
        "Fingerprints Match",
        candidate.getSarif().getVendor());
  }

  private double getSnippetSimilarity(SarifResult resultA, SarifResult resultB) {

    if (StringUtils.hasText(resultA.getSnippet()) && StringUtils.hasText(resultB.getSnippet())) {
      return codeSimilarityService.getSimilarity(resultA.getSnippet(), resultB.getSnippet());
    }

    return 0.0;
  }

  private OllamaRule getRule(String ruleId) {
    return ollamaRuleRepository.findByMetadataId(ruleId).orElseGet(this::emptyRule);
  }

  private OllamaRule emptyRule() {
    OllamaRule ollamaRule = new OllamaRule();

    ollamaRule.setEmbedding(new float[768]);

    return ollamaRule;
  }

  private SarifResult getResult(UUID sarifId, UUID resultId) {
    return resultsRepository
        .findBySarifIdAndId(sarifId, resultId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Result not found with id: " + resultId));
  }

  public double calculateCombinedSimilarity(
      double ruleSimilarity, long distance, double codeSimilarity, double pathSimilarity) {

    // Normalize distance score: 0 distance = 1.0, LINE_NUMBER_THRESHOLD = 0.0
    double distanceScore = Math.max(0.0, 1.0 - ((double) distance / LINE_NUMBER_THRESHOLD));

    // Calculate weighted average (ensures result is between 0.0 and 1.0)
    double totalWeight =
        RULE_SIMILARITY_WEIGHT + DISTANCE_WEIGHT + CODE_SIMILARITY_WEIGHT + PATH_SIMILARITY_WEIGHT;

    return ((RULE_SIMILARITY_WEIGHT * ruleSimilarity)
            + (DISTANCE_WEIGHT * distanceScore)
            + (CODE_SIMILARITY_WEIGHT * codeSimilarity)
            + (PATH_SIMILARITY_WEIGHT * pathSimilarity))
        / totalWeight;
  }
}

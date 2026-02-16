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
  private static final double RULE_SIMILARITY_WEIGHT = 0.4;
  private static final double CODE_SIMILARITY_WEIGHT = 0.3;
  private static final double DISTANCE_WEIGHT = 0.4;
  private static final double PATH_SIMILARITY_WEIGHT = 0.3;

  private static final double SIMILARITY_THRESHOLD = 0.0;

  public List<SarifResultSimilarityResponse> findSimilarResults(UUID resultId) {

    SarifResult result = getResult(resultId);

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
        "Location : "
            + result.getLocation()
            + " matched (1.0) \n"
            + "Line distance : "
            + distance
            + " \n"
            + "Rule Similarity : "
            + ruleSimilarity
            + " \n"
            + "Code Similarity : "
            + snippetSimilarity
            + " \n"
            + "Path Similarity : "
            + pathSimilarity;

    return new SarifResultSimilarityResponse(
        candidate.getId(),
        candidate.getLocation(),
        candidate.getLineNumber(),
        candidate.getRuleId(),
        candidate.getDescription(),
        candidate.getSnippet(),
        similarity,
        reason);
  }

  private double getPathSimilarity(SarifResult resultA, SarifResult resultB) {
    List<ResultPathFingerprint> fingerprintsA = resultA.getPathFingerprints();
    List<ResultPathFingerprint> fingerprintsB = resultB.getPathFingerprints();

    log.debug(
        "Comparing path fingerprints - ResultA ID: {}, count: {}, ResultB ID: {}, count: {}",
        resultA.getId(),
        fingerprintsA.size(),
        resultB.getId(),
        fingerprintsB.size());

    // If either or both lists are empty, return 0.0
    if (fingerprintsA.isEmpty() || fingerprintsB.isEmpty()) {
      log.debug("One or both path lists are empty, returning 0.0");
      return 0.0;
    }

    // Log first fingerprint from each for debugging
    if (!fingerprintsA.isEmpty() && !fingerprintsB.isEmpty()) {
      log.debug(
          "First fingerprint A: {}, First fingerprint B: {}",
          fingerprintsA.get(0).getFingerprint(),
          fingerprintsB.get(0).getFingerprint());
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
      log.debug("Position {}: {} vs {} = {}", i, fpA, fpB, isMatch);
    }

    // Return matched count divided by length of longer list
    int maxLength = Math.max(fingerprintsA.size(), fingerprintsB.size());
    double similarity = (double) matches / maxLength;

    log.debug(
        "Path similarity: {} matches out of {} total elements = {}",
        matches,
        maxLength,
        similarity);

    return similarity;
  }

  private SarifResultSimilarityResponse getFingerprintMatchesSimilarityResponse(
      SarifResult candidate) {

    return new SarifResultSimilarityResponse(
        candidate.getId(),
        candidate.getLocation(),
        candidate.getLineNumber(),
        candidate.getRuleId(),
        candidate.getDescription(),
        candidate.getSnippet(),
        1.0,
        "Fingerprints Match");
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

  private SarifResult getResult(UUID resultId) {
    return resultsRepository
        .findById(resultId)
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

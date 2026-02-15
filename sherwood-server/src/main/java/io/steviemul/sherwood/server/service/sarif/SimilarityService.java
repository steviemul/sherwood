package io.steviemul.sherwood.server.service.sarif;

import io.steviemul.sherwood.server.entity.rule.OllamaRule;
import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import io.steviemul.sherwood.server.repository.OllamaRuleRepository;
import io.steviemul.sherwood.server.repository.ResultsRepository;
import io.steviemul.sherwood.server.response.SarifResultSimilarityResponse;
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
public class SimilarityService {

  private final ResultsRepository resultsRepository;
  private final OllamaRuleRepository ollamaRuleRepository;

  private static final int LINE_NUMBER_THRESHOLD = 5;
  private static final double RULE_SIMILARITY_WEIGHT = 0.5;
  private static final double DISTANCE_WEIGHT = 0.5;
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

    OllamaRule resultRule = getRule(result.getRuleId());
    OllamaRule candidateRule = getRule(candidate.getRuleId());

    double ruleSimilarity =
        getCosineSimilarity(resultRule.getEmbedding(), candidateRule.getEmbedding());
    long distance = Math.abs(result.getLineNumber() - candidate.getLineNumber());

    double similarity = calculateCombinedSimilarity(ruleSimilarity, distance);

    StringBuilder reason =
        new StringBuilder()
            .append("Location : " + result.getLocation() + " matched (1.0) \n")
            .append("Line distance : " + distance + " \n")
            .append("Rule Similarity : " + ruleSimilarity);

    return new SarifResultSimilarityResponse(
        candidate.getId(),
        candidate.getLocation(),
        candidate.getLineNumber(),
        candidate.getRuleId(),
        similarity,
        reason.toString());
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

  public double getCosineSimilarity(float[] embeddingA, float[] embeddingB) {

    if (embeddingA.length != embeddingB.length) {
      throw new IllegalArgumentException("Embedding lengths do not match");
    }

    double dotProduct = 0, normA = 0, normB = 0;

    for (int i = 0; i < embeddingA.length; i++) {
      dotProduct += embeddingA[i] * embeddingB[i];

      normA += Math.pow(embeddingA[i], 2);
      normB += Math.pow(embeddingB[i], 2);
    }

    if (normA == 0 && normB == 0) {
      return 0.0;
    }

    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
  }

  /**
   * Calculate a combined similarity score based on rule similarity and line distance.
   *
   * @param ruleSimilarity Cosine similarity between rule embeddings (0.0 to 1.0)
   * @param distance Absolute line number distance
   * @return Combined similarity score (0.0 to 1.0)
   */
  public double calculateCombinedSimilarity(double ruleSimilarity, long distance) {
    // Normalize distance score: 0 distance = 1.0, LINE_NUMBER_THRESHOLD = 0.0
    double distanceScore = Math.max(0.0, 1.0 - ((double) distance / LINE_NUMBER_THRESHOLD));

    // Calculate weighted average
    return (RULE_SIMILARITY_WEIGHT * ruleSimilarity) + (DISTANCE_WEIGHT * distanceScore);
  }
}

package io.steviemul.sherwood.server.scoring;

import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import io.steviemul.sherwood.server.service.code.CodeSimilarityService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class CodeSimilarityCalculator implements SimilarityScoreCalculator {

  private final CodeSimilarityService codeSimilarityService;
  private final double weight;

  @Override
  public SimilarityScore getSimilarityScore(SarifResult target, SarifResult candidate) {

    double score = getSnippetSimilarity(target, candidate);

    return new SimilarityScore("Code Embedding Similarity", score, weight, true, "");
  }

  private double getSnippetSimilarity(SarifResult resultA, SarifResult resultB) {

    if (StringUtils.hasText(resultA.getSnippet()) && StringUtils.hasText(resultB.getSnippet())) {
      return codeSimilarityService.getSimilarity(resultA.getSnippet(), resultB.getSnippet());
    }

    return 0.0;
  }
}

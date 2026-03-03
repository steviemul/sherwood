package io.steviemul.sherwood.server.scoring;

import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DistanceSimilarityCalculator implements SimilarityScoreCalculator {

  private final double weight;

  @Override
  public SimilarityScore getSimilarityScore(SarifResult target, SarifResult candidate) {
    return new SimilarityScore(
        "Line Distance",
        Math.abs(target.getLineNumber() - candidate.getLineNumber()),
        weight,
        true,
        "");
  }
}

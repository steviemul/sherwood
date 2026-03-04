package io.steviemul.sherwood.server.scoring;

import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DistanceSimilarityCalculator implements SimilarityScoreCalculator {

  private static final int LINE_NUMBER_THRESHOLD = 5;
  private final double weight;

  @Override
  public SimilarityScore getSimilarityScore(SarifResult target, SarifResult candidate) {

    long distance = Math.abs(target.getLineNumber() - candidate.getLineNumber());
    double distanceScore = Math.max(0.0, 1.0 - ((double) distance / LINE_NUMBER_THRESHOLD));

    return new SimilarityScore("Line Distance", distanceScore, weight, true, "");
  }
}

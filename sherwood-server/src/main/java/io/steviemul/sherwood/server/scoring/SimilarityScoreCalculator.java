package io.steviemul.sherwood.server.scoring;

import io.steviemul.sherwood.server.entity.sarif.SarifResult;

public interface SimilarityScoreCalculator {
  SimilarityScore getSimilarityScore(SarifResult target, SarifResult candidate);
}

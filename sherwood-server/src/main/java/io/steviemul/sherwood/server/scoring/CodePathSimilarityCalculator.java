package io.steviemul.sherwood.server.scoring;

import io.steviemul.sherwood.server.entity.sarif.ResultPathFingerprint;
import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CodePathSimilarityCalculator implements SimilarityScoreCalculator {

  private final double weight;

  @Override
  public SimilarityScore getSimilarityScore(SarifResult target, SarifResult candidate) {

    double score = getPathSimilarity(target, candidate);

    return new SimilarityScore("Code Path Similarity", score, weight, true, "");
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
}

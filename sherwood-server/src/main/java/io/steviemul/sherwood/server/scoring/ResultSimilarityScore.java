package io.steviemul.sherwood.server.scoring;

import java.util.List;

public record ResultSimilarityScore(
    double availableScore, double totalScore, List<SimilarityScore> reasons) {}

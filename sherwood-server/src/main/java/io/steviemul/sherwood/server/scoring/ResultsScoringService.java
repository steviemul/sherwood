package io.steviemul.sherwood.server.scoring;

import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResultsScoringService {

  private final List<SimilarityScoreCalculator> scoringCalculators;

  public ResultSimilarityScore getSimilarityScore(SarifResult target, SarifResult candidate) {
    List<SimilarityScore> scores = getScores(target, candidate);

    double totalScoreSum =
        scores.stream().mapToDouble(score -> score.score() * score.weight()).sum();

    double totalWeights = scores.stream().mapToDouble(SimilarityScore::weight).sum();

    double availableScoreSum =
        scores.stream()
            .filter(SimilarityScore::available)
            .mapToDouble(score -> score.score() * score.weight())
            .sum();

    double availableWeights =
        scores.stream()
            .filter(SimilarityScore::available)
            .mapToDouble(SimilarityScore::weight)
            .sum();

    // Normalize both scores by dividing by their respective weight sums
    double totalScore = totalWeights > 0 ? totalScoreSum / totalWeights : 0.0;
    double availableScore = availableWeights > 0 ? availableScoreSum / availableWeights : 0.0;

    return new ResultSimilarityScore(availableScore, totalScore, scores);
  }

  private List<SimilarityScore> getScores(SarifResult target, SarifResult candidate) {

    return scoringCalculators.stream().map(c -> c.getSimilarityScore(target, candidate)).toList();
  }
}

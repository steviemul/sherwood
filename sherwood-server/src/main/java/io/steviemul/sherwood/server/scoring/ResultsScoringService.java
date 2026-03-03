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

    double totalScore = scores.stream().mapToDouble(score -> score.score() * score.weight()).sum();

    double availableWeights =
        scores.stream()
            .filter(SimilarityScore::available)
            .mapToDouble(SimilarityScore::weight)
            .sum();

    double availableScore = totalScore / availableWeights;

    return new ResultSimilarityScore(availableScore, totalScore, scores);
  }

  private List<SimilarityScore> getScores(SarifResult target, SarifResult candidate) {

    return scoringCalculators.stream().map(c -> c.getSimilarityScore(target, candidate)).toList();
  }
}

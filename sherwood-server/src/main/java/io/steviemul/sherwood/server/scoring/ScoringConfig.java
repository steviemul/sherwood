package io.steviemul.sherwood.server.scoring;

import io.steviemul.sherwood.server.repository.OllamaRuleRepository;
import io.steviemul.sherwood.server.service.code.CodeSimilarityService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScoringConfig {

  private static final double RULE_SIMILARITY_WEIGHT = 0.4;
  private static final double CODE_SIMILARITY_WEIGHT = 0.3;
  private static final double DISTANCE_WEIGHT = 0.4;
  private static final double PATH_SIMILARITY_WEIGHT = 0.3;

  @Bean
  public List<SimilarityScoreCalculator> scoringCalculators(
      OllamaRuleRepository ollamaRuleRepository, CodeSimilarityService codeSimilarityService) {

    return List.of(
        new RuleSimilarityCalculator(ollamaRuleRepository, RULE_SIMILARITY_WEIGHT),
        new CodePathSimilarityCalculator(PATH_SIMILARITY_WEIGHT),
        new CodeSimilarityCalculator(codeSimilarityService, CODE_SIMILARITY_WEIGHT),
        new DistanceSimilarityCalculator(DISTANCE_WEIGHT));
  }
}

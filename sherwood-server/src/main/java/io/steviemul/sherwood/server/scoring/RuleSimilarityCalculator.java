package io.steviemul.sherwood.server.scoring;

import static io.steviemul.sherwood.server.utils.SimilarityUtils.getCosineSimilarity;

import io.steviemul.sherwood.server.entity.rule.OllamaRule;
import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import io.steviemul.sherwood.server.repository.OllamaRuleRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RuleSimilarityCalculator implements SimilarityScoreCalculator {

  private final OllamaRuleRepository ollamaRuleRepository;
  private final double weight;

  @Override
  public SimilarityScore getSimilarityScore(SarifResult target, SarifResult candidate) {

    OllamaRule resultRule = getRule(target.getRuleId());
    OllamaRule candidateRule = getRule(candidate.getRuleId());

    double ruleSimilarity =
        getCosineSimilarity(resultRule.getEmbedding(), candidateRule.getEmbedding());

    return new SimilarityScore("Rule Embedding Similarity", ruleSimilarity, weight, true, "");
  }

  private OllamaRule getRule(String ruleId) {
    return ollamaRuleRepository.findByMetadataId(ruleId).orElseGet(this::emptyRule);
  }

  private OllamaRule emptyRule() {
    OllamaRule ollamaRule = new OllamaRule();

    ollamaRule.setEmbedding(new float[768]);

    return ollamaRule;
  }
}

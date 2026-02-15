package io.steviemul.sherwood.server.service.rules;

import io.steviemul.sherwood.server.repository.OllamaRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OllamaRulesService {

  private final OllamaRuleRepository ollamaRuleRepository;

  public boolean ruleExists(String ruleId) {
    return ollamaRuleRepository.countByRuleId(ruleId) > 0;
  }
}

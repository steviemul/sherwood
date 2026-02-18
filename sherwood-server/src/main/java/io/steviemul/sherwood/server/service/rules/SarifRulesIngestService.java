package io.steviemul.sherwood.server.service.rules;

import io.steviemul.sherwood.sarif.ReportingDescriptor;
import io.steviemul.sherwood.sarif.SarifSchema210;
import io.steviemul.sherwood.server.entity.rule.RuleRequest;
import io.steviemul.sherwood.server.service.sarif.StorageService;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class SarifRulesIngestService {

  public static final String DEFAULT_SEVERITY = "warning";

  private final StorageService storageService;
  private final RulesVectorService rulesVectorService;
  private final OllamaRulesService ollamaRulesService;

  private final Logger jobLogger = new JobRunrDashboardLogger(log);

  public void ingestSarifRules(String storageKey) {
    jobLogger.info("Ingesting sarif rules with key {}", storageKey);

    SarifSchema210 sarifFile = storageService.readSarif(storageKey);

    Set<ReportingDescriptor> rules = getSarifRules(sarifFile);

    if (CollectionUtils.isEmpty(rules)) {
      jobLogger.info("No rules found in sarif {}", storageKey);
    } else {
      processRules(rules);
    }
  }

  private void processRules(Set<ReportingDescriptor> rules) {

    jobLogger.info("Found {} rules to ingest", rules.size());

    rules.forEach(this::ingestRule);
  }

  private void ingestRule(ReportingDescriptor rule) {

    String id = rule.getId();

    if (ollamaRulesService.ruleExists(id)) {
      jobLogger.info("Rule {} already exists, no need to ingest", id);
    } else {
      ingestRule(id, rule);
    }
  }

  private void ingestRule(String id, ReportingDescriptor rule) {

    String name = rule.getName();

    String severity =
        getOrDefault(() -> rule.getDefaultConfiguration().getLevel().toString(), DEFAULT_SEVERITY);

    String shortDescription = getOrDefault(() -> rule.getShortDescription().getText(), "");
    String fullDescription = getOrDefault(() -> rule.getFullDescription().getText(), "");
    String markdown = getOrDefault(() -> rule.getHelp().getMarkdown(), "");

    if (!StringUtils.hasText(shortDescription)) shortDescription += "\n\n";
    if (!StringUtils.hasText(fullDescription)) fullDescription += "\n\n";

    String description = shortDescription + fullDescription + markdown;

    RuleRequest ruleRequest =
        new RuleRequest(
            id, name, severity, null, null, description, null, null, null, null, "steviemul");

    rulesVectorService.save(ruleRequest);

    jobLogger.info("Rule {} saved successfully in vector store", id);
  }

  private Set<ReportingDescriptor> getSarifRules(SarifSchema210 sarif) {

    try {
      return sarif.getRuns().getFirst().getTool().getDriver().getRules();
    } catch (Exception e) {
      return Collections.emptySet();
    }
  }

  private <T> T getOrDefault(Supplier<T> getter, T defaultValue) {

    try {
      return getter.get();
    } catch (Exception e) {
      return defaultValue;
    }
  }
}

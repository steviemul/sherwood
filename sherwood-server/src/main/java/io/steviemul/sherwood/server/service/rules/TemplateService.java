package io.steviemul.sherwood.server.service.rules;

import static io.steviemul.sherwood.server.utils.Sanitizer.sanitize;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.steviemul.sherwood.server.entity.rule.RuleRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TemplateService {

  private static final String RULE_TEMPLATE_RESOURCE = "templates/RuleEmbedding.template";

  private static final String ID = "id";
  private static final String NAME = "name";
  private static final String DESCRIPTION = "description";
  private static final String CATEGORY = "category";
  private static final String CWE = "cwe";
  private static final String SEVERITY = "severity";
  private static final String INFORMATION = "information";
  private static final String LANGUAGES = "languages";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public String renderRuleEmbedding(RuleRequest ruleRequest) {

    Map<String, Object> data = ruleRequestToTemplateMap(ruleRequest);

    return renderMapAsString(data);
  }

  private String renderMapAsString(Map<String, Object> data) {

    StringBuilder builder = new StringBuilder();

    for (Map.Entry<String, Object> entry : data.entrySet()) {
      if (entry.getValue() != null && StringUtils.hasText(entry.getValue().toString())) {
        builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
      }
    }

    return builder.toString();
  }

  public Map<String, Object> ruleRequestToTemplateMap(RuleRequest ruleRequest) {

    Map<String, Object> data = new HashMap<>();

    List<String> cwes = Objects.requireNonNullElse(ruleRequest.cwe(), new ArrayList<>());
    List<String> languages = Objects.requireNonNullElse(ruleRequest.language(), new ArrayList<>());

    data.put(ID, ruleRequest.id());
    data.put(NAME, ruleRequest.name());
    data.put(DESCRIPTION, ruleRequest.description());
    data.put(CATEGORY, ruleRequest.category());
    data.put(SEVERITY, ruleRequest.severity());
    data.put(CWE, String.join(", ", cwes));
    data.put(LANGUAGES, String.join(", ", languages));

    String information = sanitize(ruleRequest.risk(), ruleRequest.advice(), ruleRequest.issue());

    data.put(INFORMATION, information);

    return objectToMap(data);
  }

  public Map<String, Object> objectToMap(Object object) {

    Map<String, Object> data = objectMapper.convertValue(object, new TypeReference<>() {});

    data.entrySet().removeIf(e -> e.getValue() == null);

    return data;
  }
}

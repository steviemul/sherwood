package io.steviemul.sherwood.server.entity.rule;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RuleRequest(
    @NotBlank String id,
    String name,
    String severity,
    String category,
    List<String> cwe,
    String description,
    String risk,
    String issue,
    String advice,
    List<String> language,
    String vendor) {}

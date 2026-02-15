package io.steviemul.sherwood.server.entity.rule;

import java.util.Map;

public record DocumentResponse(double score, Map<String, Object> metadata) {}

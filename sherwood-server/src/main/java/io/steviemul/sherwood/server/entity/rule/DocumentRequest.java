package io.steviemul.sherwood.server.entity.rule;

import java.util.UUID;

public record DocumentRequest(
    String filename,
    String type,
    Integer lineNumber,
    String description,
    String code,
    String reason,
    String category,
    String severity,
    String language,
    UUID id) {}

package io.steviemul.sherwood.server.response;

import java.util.UUID;

public record SarifResultSimilarityResponse(
    UUID matchingResultId,
    UUID sarifId,
    String location,
    long lineNumber,
    String ruleId,
    String description,
    String snippet,
    double similarity,
    String reason,
    String vendor) {}

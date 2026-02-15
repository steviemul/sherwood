package io.steviemul.sherwood.server.response;

import java.util.UUID;

public record SarifResultSimilarityResponse(
    UUID matchingResultId,
    String location,
    int lineNumber,
    String ruleId,
    double similarity,
    String reason) {}

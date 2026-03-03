package io.steviemul.sherwood.server.response;

import io.steviemul.sherwood.server.scoring.ResultSimilarityScore;
import java.util.UUID;

public record SarifResultSimilarityResponse(
    UUID matchingResultId,
    UUID sarifId,
    String location,
    long lineNumber,
    String ruleId,
    String description,
    String snippet,
    String vendor,
    ResultSimilarityScore similarity) {}

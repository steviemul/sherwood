package io.steviemul.sherwood.server.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SarifResultResponse(
    UUID id,
    UUID sarifId,
    String location,
    Long lineNumber,
    String fingerprint,
    String snippet,
    String description,
    String ruleId,
    Double confidence,
    Boolean reachable,
    String graph,
    LocalDateTime created,
    LocalDateTime updated) {}

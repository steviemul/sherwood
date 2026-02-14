package io.steviemul.sherwood.server.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SarifResultResponse(
    UUID id,
    UUID sarifId,
    String location,
    Integer lineNumber,
    String fingerprint,
    String snippet,
    String description,
    String ruleId,
    LocalDateTime created,
    LocalDateTime updated) {}

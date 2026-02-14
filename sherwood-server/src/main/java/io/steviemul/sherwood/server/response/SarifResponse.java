package io.steviemul.sherwood.server.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SarifResponse(
    UUID id,
    String filename,
    String storageKey,
    String vendor,
    String repository,
    String identifier,
    LocalDateTime created,
    LocalDateTime updated) {}

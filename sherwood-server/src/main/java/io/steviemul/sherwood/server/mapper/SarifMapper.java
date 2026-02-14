package io.steviemul.sherwood.server.mapper;

import io.steviemul.sherwood.server.entity.sarif.Sarif;
import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import io.steviemul.sherwood.server.response.SarifResponse;
import io.steviemul.sherwood.server.response.SarifResultResponse;

public class SarifMapper {

  private SarifMapper() {}

  public static SarifResponse sarifEntityToSarifResponse(Sarif sarif) {
    return new SarifResponse(
        sarif.getId(),
        sarif.getFilename(),
        sarif.getStorageKey(),
        sarif.getVendor(),
        sarif.getRepository(),
        sarif.getIdentifier(),
        sarif.getCreated(),
        sarif.getUpdated());
  }

  public static SarifResultResponse sarifResultEntityToSarifResultResponse(
      SarifResult sarifResult) {

    return new SarifResultResponse(
        sarifResult.getId(),
        sarifResult.getSarif().getId(),
        sarifResult.getLocation(),
        sarifResult.getLineNumber(),
        sarifResult.getFingerprint(),
        sarifResult.getSnippet(),
        sarifResult.getDescription(),
        sarifResult.getRuleId(),
        sarifResult.getCreated(),
        sarifResult.getUpdated());
  }
}

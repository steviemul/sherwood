package io.steviemul.sherwood.server.mapper;

import io.steviemul.sherwood.server.entity.sarif.Sarif;
import io.steviemul.sherwood.server.response.SarifResponse;

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
}

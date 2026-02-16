package io.steviemul.sherwood.server.context;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SarifContext {
  private final String vendor;
  private final String repository;
  private final String identifier;
  private final String branch;
}

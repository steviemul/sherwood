package io.steviemul.sherwood.server.entity.rule;

public record QueryAdjustments(Boolean exact, Double threshold, Integer size, Boolean cache) {

  public QueryAdjustments {
    if (exact == null) exact = false;
    if (threshold == null) threshold = 0.5;
    if (size == null) size = 5;
    if (cache == null) cache = false;
  }
}

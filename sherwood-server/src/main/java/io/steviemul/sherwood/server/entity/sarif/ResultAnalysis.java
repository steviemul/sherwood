package io.steviemul.sherwood.server.entity.sarif;

public record ResultAnalysis(double confidence, boolean reachable, String graph) {}

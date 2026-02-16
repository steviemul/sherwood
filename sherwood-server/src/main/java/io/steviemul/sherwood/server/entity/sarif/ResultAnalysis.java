package io.steviemul.sherwood.server.entity.sarif;

import java.util.List;

public record ResultAnalysis(
    double confidence, boolean reachable, String graph, List<AnalysisPath> path) {}

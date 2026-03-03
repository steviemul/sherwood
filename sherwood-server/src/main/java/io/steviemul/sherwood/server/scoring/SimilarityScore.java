package io.steviemul.sherwood.server.scoring;

public record SimilarityScore(
    String title, double score, double weight, boolean available, String additionalInformation) {}

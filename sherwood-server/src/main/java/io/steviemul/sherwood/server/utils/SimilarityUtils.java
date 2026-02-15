package io.steviemul.sherwood.server.utils;

public class SimilarityUtils {

  public static double getCosineSimilarity(float[] embeddingA, float[] embeddingB) {

    if (embeddingA.length != embeddingB.length) {
      throw new IllegalArgumentException("Embedding lengths do not match");
    }

    double dotProduct = 0, normA = 0, normB = 0;

    for (int i = 0; i < embeddingA.length; i++) {
      dotProduct += embeddingA[i] * embeddingB[i];

      normA += Math.pow(embeddingA[i], 2);
      normB += Math.pow(embeddingB[i], 2);
    }

    if (normA == 0 && normB == 0) {
      return 0.0;
    }

    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
  }
}

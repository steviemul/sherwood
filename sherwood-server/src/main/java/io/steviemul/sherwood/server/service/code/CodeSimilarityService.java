package io.steviemul.sherwood.server.service.code;

import static io.steviemul.sherwood.server.utils.SimilarityUtils.getCosineSimilarity;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class CodeSimilarityService {

  private final EmbeddingModel embeddingModel;

  public CodeSimilarityService(EmbeddingModel embeddingModel) {
    this.embeddingModel = embeddingModel;
  }

  public double getSimilarity(String first, String second) {

    float[] embeddingA = embeddingModel.embed(first);
    float[] embeddingB = embeddingModel.embed(second);

    return getCosineSimilarity(embeddingA, embeddingB);
  }
}

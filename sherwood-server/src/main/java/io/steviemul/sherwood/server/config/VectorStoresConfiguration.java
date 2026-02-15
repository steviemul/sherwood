package io.steviemul.sherwood.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Slf4j
public class VectorStoresConfiguration {

  @Bean("ollamaRulesVectorStore")
  public VectorStore ollamaRulesVectorStore(
      JdbcTemplate jdbcTemplate,
      @Qualifier("ollamaTextEmbeddingModel") EmbeddingModel embeddingModel) {

    return PgVectorStore.builder(jdbcTemplate, embeddingModel)
        .vectorTableName("vector_store_ollama_rules")
        .build();
  }
}

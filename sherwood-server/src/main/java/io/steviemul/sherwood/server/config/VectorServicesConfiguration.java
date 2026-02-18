package io.steviemul.sherwood.server.config;

import io.steviemul.sherwood.server.service.rules.RulesVectorService;
import io.steviemul.sherwood.server.service.rules.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class VectorServicesConfiguration {

  private final TemplateService templateService;

  @Bean("ollamaRulesVectorService")
  public RulesVectorService ollamaRulesVectorService(
      @Qualifier("ollamaRulesVectorStore") VectorStore vectorStore) {
    return new RulesVectorService(vectorStore, templateService);
  }
}

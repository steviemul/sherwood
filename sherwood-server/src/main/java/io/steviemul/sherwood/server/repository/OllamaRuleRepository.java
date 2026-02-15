package io.steviemul.sherwood.server.repository;

import io.steviemul.sherwood.server.entity.rule.OllamaRule;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OllamaRuleRepository extends JpaRepository<OllamaRule, UUID> {

  @Query(
      value = "SELECT COUNT(*) FROM vector_store_ollama_rules WHERE metadata->>'id' = :ruleId",
      nativeQuery = true)
  int countByRuleId(@Param("ruleId") String ruleId);

  @Query(
      value =
          """
        SELECT * FROM vector_store_ollama_rules
        WHERE metadata ->> 'id' = :ruleId
        """,
      nativeQuery = true)
  Optional<OllamaRule> findByMetadataId(@Param("ruleId") String ruleId);
}

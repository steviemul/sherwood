package io.steviemul.sherwood.server.repository;

import io.steviemul.sherwood.server.entity.rule.OllamaRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OllamaRuleRepository extends JpaRepository<OllamaRule, UUID> {

  @Query(
      value =
          "SELECT id, content, metadata, NULL as embedding FROM vector_store_ollama_rules WHERE metadata->>'vendor' = :vendor",
      nativeQuery = true)
  List<OllamaRule> findAllByVendor(@Param("vendor") String vendor);

  @Query(
      value = "SELECT COUNT(*) FROM vector_store_ollama_rules WHERE metadata->>'vendor' = :vendor",
      nativeQuery = true)
  int countAllByVendor(@Param("vendor") String vendor);

  @Query(
      value =
          "SELECT id, content, metadata, NULL as embedding FROM vector_store_ollama_rules WHERE metadata->>'id' = ANY(:ruleIds)",
      nativeQuery = true)
  List<OllamaRule> findAllByRuleIdIn(@Param("ruleIds") List<String> ruleIds);

  @Query(
      value =
          "SELECT id, content, metadata, NULL as embedding FROM vector_store_ollama_rules WHERE metadata->>'id' = :ruleId",
      nativeQuery = true)
  Optional<OllamaRule> findByRuleId(@Param("ruleId") String ruleId);
}

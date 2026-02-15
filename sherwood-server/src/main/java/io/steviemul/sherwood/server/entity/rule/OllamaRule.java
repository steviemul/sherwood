package io.steviemul.sherwood.server.entity.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity mapping for the table vector_store_ollama_rules
 *
 * <p>Schema: id uuid DEFAULT uuid_generate_v4() PRIMARY KEY, content text, metadata json, embedding
 * vector(768)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "vector_store_ollama_rules")
public class OllamaRule {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(columnDefinition = "text")
  private String content;

  // For JSON, simplest is to store as string; can be changed to JsonNode or Map with proper type
  // mapping
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private String metadata;

  @JdbcTypeCode(SqlTypes.VECTOR)
  @Column(name = "embedding", columnDefinition = "vector(768)")
  private float[] embedding;
}

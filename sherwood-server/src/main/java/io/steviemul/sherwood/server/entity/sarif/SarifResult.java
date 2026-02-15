package io.steviemul.sherwood.server.entity.sarif;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SarifResult {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sarif", nullable = false)
  private Sarif sarif;

  @Column(name = "location", nullable = false)
  private String location;

  @Column(name = "line_number", nullable = false)
  private Long lineNumber;

  @Column(name = "fingerprint")
  private String fingerprint;

  @Column(name = "snippet", columnDefinition = "TEXT")
  private String snippet;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "rule_id", nullable = false)
  private String ruleId;

  @Column(name = "confidence")
  @Builder.Default
  private Double confidence = 0.0;

  @Column(name = "reachable")
  @Builder.Default
  private Boolean reachable = false;

  @Column(name = "graph", columnDefinition = "TEXT")
  private String graph;

  @CreationTimestamp
  @Column(name = "created", nullable = false, updatable = false)
  private LocalDateTime created;

  @UpdateTimestamp
  @Column(name = "updated", nullable = false)
  private LocalDateTime updated;
}

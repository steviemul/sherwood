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
@Table(name = "sarifs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sarif {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "filename", nullable = false)
  private String filename;

  @Column(name = "storage_key", nullable = false)
  private String storageKey;

  @Column(name = "vendor", nullable = false)
  private String vendor;

  @Column(name = "repository", nullable = false)
  private String repository;

  @Column(name = "identifier", nullable = false)
  private String identifier;

  @CreationTimestamp
  @Column(name = "created", nullable = false, updatable = false)
  private LocalDateTime created;

  @UpdateTimestamp
  @Column(name = "updated", nullable = false)
  private LocalDateTime updated;
}

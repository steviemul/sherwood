package io.steviemul.sherwood.server.entity.sarif;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "result_path_fingerprints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultPathFingerprint {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "fingerprint", nullable = false)
  private String fingerprint;

  @Column(name = "fingerprint_order", nullable = false)
  private Integer fingerprintOrder;

  @CreationTimestamp
  @Column(name = "created", nullable = false, updatable = false)
  private LocalDateTime created;
}

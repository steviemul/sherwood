package io.steviemul.sherwood.server.repository;

import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultsRepository extends JpaRepository<SarifResult, UUID> {

  List<SarifResult> findBySarifId(UUID sarifId);

  Optional<SarifResult> findBySarifIdAndId(UUID sarifId, UUID id);

  List<SarifResult> findBySarifRepositoryAndLocationContainingIgnoreCase(
      String sarifRepository, String location);
}

package io.steviemul.sherwood.server.repository;

import io.steviemul.sherwood.server.entity.sarif.Sarif;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SarifRepository extends JpaRepository<Sarif, UUID> {

  List<Sarif> findByRepository(String repository);

  List<Sarif> findByIdentifier(String identifier);

  List<Sarif> findByFilename(String filename);
}

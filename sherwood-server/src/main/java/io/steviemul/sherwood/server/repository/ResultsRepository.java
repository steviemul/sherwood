package io.steviemul.sherwood.server.repository;

import io.steviemul.sherwood.server.entity.sarif.SarifResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultsRepository extends JpaRepository<SarifResult, UUID> {

  List<SarifResult> findByLocation(String location);

  List<SarifResult> findByLineNumber(Integer lineNumber);

  List<SarifResult> findByRuleId(String ruleId);

  List<SarifResult> findBySarifId(UUID sarifId);
}

package io.steviemul.sherwood.server.repository;

import io.steviemul.sherwood.server.entity.sarif.ResultPathFingerprint;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultPathFingerprintRepository
    extends JpaRepository<ResultPathFingerprint, UUID> {}

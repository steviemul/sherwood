package io.steviemul.sherwood.server.service;

import io.steviemul.sherwood.server.entity.sarif.Sarif;
import io.steviemul.sherwood.server.mapper.SarifMapper;
import io.steviemul.sherwood.server.repository.SarifRepository;
import io.steviemul.sherwood.server.response.SarifResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SarifService {

  private final SarifRepository sarifRepository;

  public Sarif createSarif(String filename, String storageKey) {

    Sarif sarif =
        Sarif.builder()
            .filename(filename)
            .storageKey(storageKey)
            .vendor("steviemul")
            .repository("steviemul/webgoat")
            .identifier("ad67gdsg")
            .build();

    return sarifRepository.save(sarif);
  }

  public SarifResponse getSarifResponseById(UUID id) {
    return sarifRepository
        .findById(id)
        .map(SarifMapper::sarifEntityToSarifResponse)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Sarif not found with id: " + id));
  }
}

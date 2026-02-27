package io.steviemul.sherwood.server.service.sarif;

import io.steviemul.sherwood.sarif.PropertyBag;
import io.steviemul.sherwood.sarif.SarifSchema210;
import io.steviemul.sherwood.server.context.SarifContext;
import io.steviemul.sherwood.server.entity.sarif.Sarif;
import io.steviemul.sherwood.server.mapper.SarifMapper;
import io.steviemul.sherwood.server.repository.SarifRepository;
import io.steviemul.sherwood.server.response.SarifResponse;
import io.steviemul.sherwood.server.response.SarifResultResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private final ResultsService resultsService;
  private final StorageService storageService;

  private static final String UNRESOLVED = "unresolved";
  private static final String SHERWOOD = "sherwood";
  private static final String CONTEXT = "context";
  private static final String UNKNOWN = "unknown";
  private static final String REPOSITORY = "repository";
  private static final String IDENTIFIER = "identifier";
  private static final String BRANCH = "branch";

  public Sarif createSarif(String filename, String storageKey) {

    Sarif sarif =
        Sarif.builder()
            .filename(filename)
            .storageKey(storageKey)
            .vendor(UNRESOLVED)
            .repository(UNRESOLVED)
            .identifier(UNRESOLVED)
            .build();

    return sarifRepository.save(sarif);
  }

  public void processSarif(SarifSchema210 sarifFile, UUID sarifId) {

    Sarif sarif = sarifRepository.findById(sarifId).get();

    SarifContext context = getSarifContext(sarifFile);

    sarif.setRepository(context.getRepository());
    sarif.setVendor(context.getVendor());
    sarif.setIdentifier(context.getIdentifier());

    sarifRepository.save(sarif);

    resultsService.processResults(sarifFile, sarif);
  }

  private SarifContext getSarifContext(SarifSchema210 sarif) {

    String vendor = sarif.getRuns().getFirst().getTool().getDriver().getName();

    PropertyBag properties = Optional.ofNullable(sarif.getProperties()).orElse(new PropertyBag());

    Map<String, String> context = getSherwoodContext(properties);

    String repo = context.getOrDefault(REPOSITORY, UNKNOWN);
    String identifier = context.getOrDefault(IDENTIFIER, UNKNOWN);
    String branch = context.getOrDefault(BRANCH, UNKNOWN);

    return SarifContext.builder()
        .vendor(vendor)
        .repository(repo)
        .identifier(identifier)
        .branch(branch)
        .build();
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> getSherwoodContext(PropertyBag properties) {

    try {
      Map<String, Object> sherwood =
          (Map<String, Object>) properties.getAdditionalProperties().get(SHERWOOD);

      return (Map<String, String>) sherwood.getOrDefault(CONTEXT, Collections.emptyMap());
    } catch (Exception e) {
      return Collections.emptyMap();
    }
  }

  public List<SarifResponse> getAllSarifs() {
    return sarifRepository.findAll().stream()
        .map(SarifMapper::sarifEntityToSarifResponse)
        .map(this::addDownloadUrl)
        .toList();
  }

  public SarifResponse getSarifResponseById(UUID id) {
    return sarifRepository
        .findById(id)
        .map(SarifMapper::sarifEntityToSarifResponse)
        .map(this::addDownloadUrl)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Sarif not found with id: " + id));
  }

  public void deleteSarif(UUID id) {

    resultsService.deleteResultsBySarif(id);
    sarifRepository.deleteById(id);
  }

  private SarifResponse addDownloadUrl(SarifResponse sarifResponse) {

    String downloadUrl = storageService.getDownloadUrl(sarifResponse.storageKey());

    return new SarifResponse(
        sarifResponse.id(),
        sarifResponse.filename(),
        sarifResponse.storageKey(),
        downloadUrl,
        sarifResponse.vendor(),
        sarifResponse.repository(),
        sarifResponse.identifier(),
        sarifResponse.created(),
        sarifResponse.updated());
  }

  public SarifResultResponse getResultById(UUID sarifId, UUID resultId) {
    return resultsService
        .getResultBySarifIfAndId(sarifId, resultId)
        .map(SarifMapper::sarifResultEntityToSarifResultResponse)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Result not found with id: " + resultId));
  }

  public List<SarifResultResponse> getResultsBySarifId(UUID sarifId) {
    return resultsService.getResultsBySarifId(sarifId).stream()
        .map(SarifMapper::sarifResultEntityToSarifResultResponse)
        .toList();
  }
}

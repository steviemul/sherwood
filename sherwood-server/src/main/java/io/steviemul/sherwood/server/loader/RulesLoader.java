package io.steviemul.sherwood.server.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.steviemul.sherwood.server.entity.rule.RuleRequest;
import io.steviemul.sherwood.server.service.rules.OllamaRulesService;
import io.steviemul.sherwood.server.service.rules.RulesVectorService;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RulesLoader implements ApplicationRunner {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final RulesVectorService ollamaRulesVectorService;
  private final OllamaRulesService ollamaRulesService;

  private static final String RULES_DIRECTORY = "rules";
  private final ExecutorService executor = Executors.newFixedThreadPool(4);

  @Override
  public void run(ApplicationArguments args) {
    log.info("RulesLoader starting up");

    List<Path> zipFiles = getRulesZipFiles();

    zipFiles.forEach(this::processZipFile);

    log.info("RulesLoader completed");
  }

  private void processZipFile(Path zipFile) {

    log.info("Checking {} for new rules", zipFile.getFileName());

    List<RuleRequest> ruleRequests = getRuleRequests(zipFile);

    log.info("Found {} rules in {}", ruleRequests.size(), zipFile.getFileName());

    List<RuleRequest> newRuleRequests = getNewRuleRequests(ruleRequests);

    if (!newRuleRequests.isEmpty()) {

      log.info("Found {} new rules to save", newRuleRequests.size());

      List<Future<?>> futures = batchSaveRuleRequests(newRuleRequests);

      waitForFutures(futures);

      log.info("Rules saved");
    } else {
      log.info("No new rules to save");
    }
  }

  private List<RuleRequest> getNewRuleRequests(List<RuleRequest> ruleRequests) {

    return ruleRequests.stream().filter(r -> !ollamaRulesService.ruleExists(r.id())).toList();
  }

  private void waitForFutures(List<Future<?>> futures) {
    try {
      for (Future<?> future : futures) {
        future.get();
      }
    } catch (Exception ignored) {
    }
  }

  private List<Future<?>> batchSaveRuleRequests(List<RuleRequest> ruleRequests) {

    List<Future<?>> futures = new ArrayList<>();

    final int batchSize = 100;

    for (int start = 0; start < ruleRequests.size(); start += batchSize) {
      int end = Math.min(start + batchSize, ruleRequests.size());
      List<RuleRequest> batch = ruleRequests.subList(start, end);

      try {
        int finalStart = start;
        Future<?> future =
            executor.submit(
                () -> {
                  ollamaRulesVectorService.save(batch);
                  log.info("Saved batch {}-{} of {}", finalStart + 1, end, ruleRequests.size());
                });

        futures.add(future);

      } catch (Exception e) {
        log.error(
            "Failed to save batch {}-{} (size {}): {}",
            start + 1,
            end,
            batch.size(),
            e.getMessage(),
            e);
        throw e;
      }
    }

    return futures;
  }

  private List<RuleRequest> getRuleRequests(Path rulesFile) {

    List<RuleRequest> ruleRequests = new ArrayList<>();

    try (ZipFile zipFile = new ZipFile(rulesFile.toFile())) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();

        if (!entry.isDirectory()) {
          try (InputStream inputStream = zipFile.getInputStream(entry)) {
            byte[] contents = inputStream.readAllBytes();

            RuleRequest ruleRequest = getRuleContents(contents);
            ruleRequests.add(ruleRequest);
          }
        }
      }
    } catch (Exception e) {
      log.error("Import rules failed", e);
    }

    return ruleRequests;
  }

  private RuleRequest getRuleContents(byte[] contents) throws Exception {

    return objectMapper.readValue(contents, RuleRequest.class);
  }

  private List<Path> getRulesZipFiles() {

    try {
      File rulesDirectory = new ClassPathResource(RULES_DIRECTORY).getFile();

      return Arrays.stream(rulesDirectory.list())
          .map(child -> Path.of(rulesDirectory.getAbsolutePath(), child))
          .toList();
    } catch (Exception e) {
      log.error("Error loading rules directory", e);
    }

    return Collections.emptyList();
  }
}

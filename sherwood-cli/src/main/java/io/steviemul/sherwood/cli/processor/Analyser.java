package io.steviemul.sherwood.cli.processor;

import io.steviemul.sherwood.cli.context.LocalContextProvider;
import io.steviemul.sherwood.cli.logging.Logger;
import io.steviemul.sherwood.parsers.CallGraph;
import io.steviemul.sherwood.parsers.LanguageParser;
import io.steviemul.sherwood.parsers.Location;
import io.steviemul.sherwood.parsers.MethodSignature;
import io.steviemul.sherwood.parsers.ParsedFile;
import io.steviemul.sherwood.parsers.PathNode;
import io.steviemul.sherwood.parsers.ReachabilityResult;
import io.steviemul.sherwood.sarif.PropertyBag;
import io.steviemul.sherwood.sarif.Result;
import io.steviemul.sherwood.sarif.SarifSchema210;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
@Builder
public class Analyser {

  private final Path sourceCodeRoot;
  private final Path sarifPath;
  private final Path outputPath;
  private final String serverUrl;

  private final Map<String, LanguageParser> languageParsers =
      Map.of("java", new io.steviemul.sherwood.parsers.java.JavaLanguageParser());

  public void analyse() {

    Date start = new Date();

    Logger.info("Reading source code");

    RootScanner rootScanner = new RootScanner(sourceCodeRoot);

    List<Path> codeFiles = rootScanner.getAllMatchingFiles();

    Logger.taskComplete("Found " + codeFiles.size() + " files");

    SarifProcessor sarifProcessor = new SarifProcessor(sarifPath);

    SarifSchema210 sarif = sarifProcessor.readSarif();

    List<Result> results = getResults(sarif);

    Logger.taskComplete("Sarif read. Found " + results.size() + " results");

    LanguageParser parser = languageParsers.get("java");

    List<ParsedFile> parsedFiles = new ArrayList<>();

    try (ProgressBar parsedFilesProgress =
        Logger.createProgressBar("  Parsing Files", codeFiles.size())) {

      for (int i = 0; i < codeFiles.size(); i++) {
        parsedFiles.add(parser.parse(codeFiles.get(i)));

        parsedFilesProgress.step();
        parsedFilesProgress.setExtraMessage("Parsed " + i + " of " + codeFiles.size() + " files");
      }
    }

    Logger.taskComplete("Parsed " + parsedFiles.size() + " files");

    CallGraph graph = parser.buildCallGraph(parsedFiles);

    Logger.taskComplete("Built call graph");

    List<ReachabilityResult> reachabilityResults = new ArrayList<>();

    Logger.info("Performing reachability analysis on " + results.size() + " results");

    for (Result result : results) {
      Location location = getLocation(result);

      ReachabilityResult reachabilityResult = parser.findReachability(graph, parsedFiles, location);

      if (reachabilityResult.completed()) {
        reachabilityResults.add(reachabilityResult);

        addReachabilityAnalysisToResult(result, reachabilityResult);
      }
    }

    Logger.taskComplete("Analysed " + reachabilityResults.size() + " results");

    LocalContextProvider localContextProvider = new LocalContextProvider(sourceCodeRoot);

    localContextProvider.addLocalContext(sarif);

    sarifProcessor.writeSarif(sarif, outputPath);

    Logger.taskComplete("Updated sarif results written to " + outputPath);

    if (!StringUtils.isEmpty(serverUrl)) {
      SarifUploader sarifUploader = new SarifUploader(outputPath, serverUrl);

      sarifUploader.uploadSarif();

      Logger.taskComplete("Sarif uploaded to " + serverUrl);
    }

    Date end = new Date();

    Logger.taskComplete("Analysis complete, took " + (end.getTime() - start.getTime()) + "ms");

    printSummary(reachabilityResults);
  }

  private void printSummary(List<ReachabilityResult> results) {

    Map<String, String> data = new LinkedHashMap<>();

    data.put("Processed Results", String.valueOf(results.size()));

    results.stream()
        .collect(Collectors.groupingBy(ReachabilityResult::confidence, Collectors.counting()))
        .entrySet()
        .stream()
        .sorted(Map.Entry.<Double, Long>comparingByKey(Comparator.reverseOrder()))
        .forEach(
            entry -> data.put("Confidence " + entry.getKey(), String.valueOf(entry.getValue())));

    Logger.printSummaryTable("Analysis Summary", data);
  }

  private void addReachabilityAnalysisToResult(
      Result sarifResult, ReachabilityResult reachabilityResult) {

    PropertyBag properties =
        Optional.ofNullable(sarifResult.getProperties()).orElse(new PropertyBag());

    String graph =
        Base64.getEncoder()
            .encodeToString(reachabilityResult.toMermaid().getBytes(StandardCharsets.UTF_8));

    Map<String, Object> analysis = new LinkedHashMap<>();

    analysis.put("confidence", reachabilityResult.confidence());
    analysis.put("reachable", reachabilityResult.isReachable());
    analysis.put("graph", graph);

    if (reachabilityResult.path() != null) {
      analysis.put(
          "path", reachabilityResult.path().stream().map(this::toOutputSignature).toList());
    }

    properties.setAdditionalProperty("analysis", analysis);

    sarifResult.setProperties(properties);
  }

  private Location getLocation(Result result) {
    io.steviemul.sherwood.sarif.Location sarifLocation = result.getLocations().getFirst();

    // Convert SARIF location to parsers location
    String uri = sarifLocation.getPhysicalLocation().getArtifactLocation().getUri();
    int lineNumber = sarifLocation.getPhysicalLocation().getRegion().getStartLine().intValue();
    Path filePath = sourceCodeRoot.resolve(uri);

    return new Location(filePath, lineNumber);
  }

  private List<Result> getResults(SarifSchema210 sarifSchema210) {
    return sarifSchema210.getRuns().getFirst().getResults();
  }

  private OutputSignature toOutputSignature(PathNode pathNode) {

    MethodSignature methodSignature = pathNode.method();

    return new OutputSignature(
        methodSignature.name(), methodSignature.qualifiedName(), methodSignature.parameters());
  }

  private record OutputSignature(String name, String qualifiedName, List<String> parameters) {}
}

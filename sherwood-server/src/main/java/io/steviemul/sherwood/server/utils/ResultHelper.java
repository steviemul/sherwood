package io.steviemul.sherwood.server.utils;

import io.steviemul.sherwood.sarif.Fingerprints;
import io.steviemul.sherwood.sarif.PartialFingerprints;
import io.steviemul.sherwood.sarif.Result;
import io.steviemul.sherwood.server.entity.sarif.AnalysisPath;
import io.steviemul.sherwood.server.entity.sarif.ResultAnalysis;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ResultHelper {

  public static final String ANALYSIS = "analysis";
  public static final String CONFIDENCE = "confidence";
  public static final String REACHABLE = "reachable";
  public static final String GRAPH = "graph";
  public static final String PATH = "path";

  private static final List<String> SUPPORTED_FINGERPRINTS =
      List.of("SCAN_LSH/v1", "matchBasedId/v1", "0", "1");

  private ResultHelper() {}

  public static String getSnippet(Result result) {
    return getOrDefault(
        () ->
            result
                .getLocations()
                .getFirst()
                .getPhysicalLocation()
                .getRegion()
                .getSnippet()
                .getText(),
        "");
  }

  public static String getFingerprint(Result result) {

    Map<String, String> fingerprints = new HashMap<>();

    fingerprints.putAll(getFingerprints(result));
    fingerprints.putAll(getPartialFingerprints(result));

    return fingerprints.entrySet().stream()
        .filter(entry -> SUPPORTED_FINGERPRINTS.contains(entry.getKey()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse("");
  }

  public static Map<String, String> getFingerprints(Result result) {
    return getOrDefault(
        () ->
            Optional.ofNullable(result.getFingerprints())
                .map(Fingerprints::getAdditionalProperties)
                .orElse(new HashMap<>()),
        Collections.emptyMap());
  }

  public static Map<String, String> getPartialFingerprints(Result result) {
    return getOrDefault(
        () ->
            Optional.ofNullable(result.getPartialFingerprints())
                .map(PartialFingerprints::getAdditionalProperties)
                .orElse(new HashMap<>()),
        Collections.emptyMap());
  }

  public static String getLocation(Result result) {
    return getOrDefault(
        () -> result.getLocations().getFirst().getPhysicalLocation().getArtifactLocation().getUri(),
        "");
  }

  public static long getLineNumber(Result result) {
    return getOrDefault(
        () -> result.getLocations().getFirst().getPhysicalLocation().getRegion().getStartLine(),
        0L);
  }

  @SuppressWarnings("unchecked")
  public static ResultAnalysis getAnalysis(Result result) {

    Map<String, Object> analysis =
        (Map<String, Object>)
            result
                .getProperties()
                .getAdditionalProperties()
                .getOrDefault(ANALYSIS, Collections.emptyMap());

    return new ResultAnalysis(
        (double) analysis.getOrDefault(CONFIDENCE, -1.0),
        (boolean) analysis.getOrDefault(REACHABLE, false),
        (String) analysis.getOrDefault(GRAPH, ""),
        getAnalysisPath(analysis.get(PATH)));
  }

  @SuppressWarnings("unchecked")
  private static List<AnalysisPath> getAnalysisPath(Object object) {
    if (object == null) {
      return Collections.emptyList();
    }

    try {
      List<Map<String, Object>> pathList = (List<Map<String, Object>>) object;

      return pathList.stream()
          .map(
              pathElement -> {
                String name = (String) pathElement.get("name");
                String qualifiedName = (String) pathElement.get("qualifiedName");
                List<String> parameters =
                    (List<String>) pathElement.getOrDefault("parameters", Collections.emptyList());

                return AnalysisPath.builder()
                    .name(name)
                    .qualifiedName(qualifiedName)
                    .parameters(parameters)
                    .build();
              })
          .toList();
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  private static <T> T getOrDefault(Supplier<T> getter, T defaultValue) {

    try {
      return getter.get();
    } catch (Exception e) {
      return defaultValue;
    }
  }
}

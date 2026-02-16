package io.steviemul.sherwood.cli.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.steviemul.sherwood.cli.utils.CommandUtils;
import io.steviemul.sherwood.sarif.PropertyBag;
import io.steviemul.sherwood.sarif.SarifSchema210;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class RepositoryContextResolver {

  public static final String CONTEXT_FILE = ".sherwoodrc.json";
  public static final String SHERWOOD = "sherwood";
  public static final String CONTEXT = "context";

  private static final String[] GIT_COMMAND_STATUS = new String[] {"git", "status"};
  private static final String[] GIT_COMMAND_REPO = new String[] {"git", "remote", "-v"};
  private static final String[] GIT_COMMAND_COMMIT = new String[] {"git", "rev-parse", "HEAD"};
  private static final String[] GIT_COMMAND_BRANCH =
      new String[] {"git", "rev-parse", "--abbrev-ref", "HEAD"};

  private static final String UNKNOWN = "unknown";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Path sourceCodeRoot;

  public void addLocalContext(SarifSchema210 sarif) {
    SherwoodConfig config = getLocalContext();

    if (config == null) {
      config = getDefaultConfig();
    }

    addContextToSarif(config, sarif);
  }

  private SherwoodConfig getDefaultConfig() {
    return new SherwoodConfig(sourceCodeRoot.toFile().getName(), UNKNOWN, UNKNOWN);
  }

  private void addContextToSarif(SherwoodConfig config, SarifSchema210 sarif) {

    PropertyBag properties = Optional.ofNullable(sarif.getProperties()).orElse(new PropertyBag());

    sarif.setProperties(properties);

    Map<String, Object> context = Map.of(CONTEXT, config);

    properties.setAdditionalProperty(SHERWOOD, context);
  }

  private SherwoodConfig getLocalContext() {

    SherwoodConfig config = getLocalSherwoodConfig();

    if (config == null) {
      config = getGitRepositoryContext();
    }

    return config;
  }

  private SherwoodConfig getLocalSherwoodConfig() {

    Path configPath = Path.of(sourceCodeRoot.toString(), CONTEXT_FILE);

    if (Files.exists(configPath)) {
      try (InputStream is = new FileInputStream(configPath.toFile())) {
        return objectMapper.readValue(is, SherwoodConfig.class);
      } catch (Exception e) {
        log.error("Error reading local config path", e);
      }
    }

    return null;
  }

  private SherwoodConfig getGitRepositoryContext() {

    File root = sourceCodeRoot.toFile();

    Optional<String> status = CommandUtils.runCommand(root, GIT_COMMAND_STATUS);

    if (status.isPresent()) {
      String repo = getRepository(root);
      String commit = getCommit(root);
      String branch = getBranch(root);

      return new SherwoodConfig(repo, commit, branch);
    }

    return null;
  }

  private String getRepository(File root) {

    Optional<String> remotes = CommandUtils.runCommand(root, GIT_COMMAND_REPO);

    return remotes.map(r -> r.split("\n")[0]).map(line -> line.split("\\s+")[1]).orElse(UNKNOWN);
  }

  private String getCommit(File root) {

    Optional<String> commit = CommandUtils.runCommand(root, GIT_COMMAND_COMMIT);

    return commit.map(String::trim).orElse(UNKNOWN);
  }

  private String getBranch(File root) {
    Optional<String> branch = CommandUtils.runCommand(root, GIT_COMMAND_BRANCH);

    return branch.map(String::trim).orElse(UNKNOWN);
  }

  private record SherwoodConfig(String repository, String identifier, String branch) {}
}

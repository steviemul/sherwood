package io.steviemul.sherwood.cli.context;

import io.steviemul.sherwood.sarif.Result;
import io.steviemul.sherwood.sarif.SarifSchema210;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class LocalContextProvider {

  private final Path sourceCodeRoot;

  public void addLocalContext(SarifSchema210 sarif) {

    CodeContextResolver codeContextResolver = new CodeContextResolver(sourceCodeRoot);

    List<Result> results = sarif.getRuns().getFirst().getResults();

    results.forEach(codeContextResolver::addCodeSnippetIfRequired);

    RepositoryContextResolver repositoryContextResolver =
        new RepositoryContextResolver(sourceCodeRoot);

    repositoryContextResolver.addLocalContext(sarif);
  }
}

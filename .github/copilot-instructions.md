# Copilot instructions for `sherwood`

## Build / test / lint (Maven)

This is a multi-module Maven repo (Java 21):
- `sarif-model`: generated SARIF schema Java model (jsonschema2pojo)
- `sherwood-server`: Spring Boot (WebFlux) service

### Common commands (from repo root)

```bash
# Build everything (includes fmt check during process-resources)
mvn clean verify

# Build without tests
mvn -DskipTests package

# Format/lint check (configured via com.spotify.fmt:fmt-maven-plugin)
# Runs automatically as part of the build, but can be run directly:
mvn com.spotify.fmt:fmt-maven-plugin:check

# Auto-fix formatting
mvn com.spotify.fmt:fmt-maven-plugin:format
```

### Run tests

There are currently no `src/test/java` tests checked in; when tests are added, use Surefire’s standard selectors:

```bash
# Run all tests
mvn test

# Run a single test class
mvn -Dtest=MyTest test

# Run a single test method
mvn -Dtest=MyTest#myMethod test
```

### Run the server

```bash
# Run Spring Boot app
mvn -pl sherwood-server spring-boot:run

# Or package and run the jar
mvn -pl sherwood-server package
java -jar sherwood-server/target/*.jar
```

## High-level architecture

### Modules

- **`sarif-model/`**
  - Generates Java POJOs from `sarif-model/schema/sarif-schema-2.1.0.json`.
  - Generation is done by `jsonschema2pojo-maven-plugin` during `generate-sources`, and generated sources are added via `build-helper-maven-plugin`.
  - The resulting package is `io.steviemul.sherwood.sarif`.

- **`sherwood-server/`**
  - Spring Boot application entrypoint: `io.steviemul.sherwood.server.ServerApplication`.
  - Exposes HTTP endpoints via annotated controllers (currently `SherwoodController`).
  - Route strings are centralized in `io.steviemul.sherwood.server.constant.Routes` (e.g., base path `/sherwood`).
  - Uses PostgreSQL + Flyway migrations; configuration is in `sherwood-server/src/main/resources/application.yml`.

### Runtime dependencies (local dev)

- Postgres with **pgvector** (for Spring AI pgvector store)
- Optional **Ollama** (for Spring AI Ollama client)

A `docker-compose.yml` is provided to run both services locally:

```bash
docker compose up -d
```

The service is configured (by default) to connect to:
- Postgres: `jdbc:postgresql://localhost:5432/sherwood_db` (user `postgres`, password `password`)
- Server port: `12080`

Flyway migrations live in `sherwood-server/src/main/resources/db/migration/` and use a custom prefix (`T...`) configured in `application.yml`.

## Key conventions (repo-specific)

- **Formatting is enforced by Maven** via `com.spotify.fmt:fmt-maven-plugin` and runs during `process-resources` (`fmt:check`). To auto-fix formatting, run `mvn com.spotify.fmt:fmt-maven-plugin:format`.

- **Generated code conventions**:
  - `sarif-model` sources are generated from JSON schema; avoid hand-editing files under `sarif-model/src/main/java/io/steviemul/sherwood/sarif/`.
  - Lombok is configured repo-wide via `lombok.config` to add `@lombok.Generated` to generated classes and to add `@ConstructorProperties` for JSON deserialization.

- **Routing constants**: route paths are composed from `Routes.BASE` + per-endpoint constants (e.g., `STATUS_ROUTE`). Prefer reusing these constants rather than duplicating strings.

- **Flyway naming**: migrations use the `T` prefix (see `spring.flyway.sql-migration-prefix`), and Flyway is configured as `out-of-order: true` with `validate-on-migrate: false`.

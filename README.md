```
   🏹
    \\
     \\        ███████╗██╗  ██╗███████╗██████╗ ██╗    ██╗ ██████╗  ██████╗ ██████╗
      \\       ██╔════╝██║  ██║██╔════╝██╔══██╗██║    ██║██╔═══██╗██╔═══██╗██╔══██╗
       ==>     ███████╗███████║█████╗  ██████╔╝██║ █╗ ██║██║   ██║██║   ██║██║  ██║
       //      ╚════██║██╔══██║██╔══╝  ██╔══██╗██║███╗██║██║   ██║██║   ██║██║  ██║
      //       ███████║██║  ██║███████╗██║  ██║╚███╔███╔╝╚██████╔╝╚██████╔╝██████╔╝
     //        ╚══════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝ ╚══╝╚══╝  ╚═════╝  ╚═════╝ ╚═════╝

            Taking from noisy SARIF • Giving actionable security insight
```

**Sherwood** is a security analysis tool that processes SARIF (Static Analysis Results Interchange Format) files to provide actionable insights through reachability analysis and vulnerability prioritization.

## Overview

Security scanners often produce overwhelming amounts of results in SARIF format. Sherwood helps you cut through the noise by:

- **Reachability Analysis**: Determines if vulnerable code is actually reachable from application entry points
- **Call Graph Visualization**: Generates DOT (Graphviz) and Mermaid diagrams showing execution paths
- **Code Context**: Extracts and displays relevant source code snippets for each finding
- **Multi-Language Support**: Extensible parser architecture (currently supports Java with ANTLR4)

## Architecture

Sherwood is a multi-module Maven project (Java 21):

- **`sarif-model`**: Auto-generated Java POJOs from SARIF 2.1.0 JSON schema
- **`parsers`**: Language parsers using ANTLR4 for call graph analysis and reachability detection
- **`sherwood-server`**: Spring Boot (WebFlux) REST API for analysis services
- **`sherwood-cli`**: Command-line interface for processing SARIF files

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.6+
- Docker & Docker Compose (for local development)

### Build

```bash
# Build all modules
mvn clean install

# Build without tests
mvn -DskipTests package

# Format code
mvn com.spotify.fmt:fmt-maven-plugin:format
```

### Run Locally

Start PostgreSQL (with pgvector) and Ollama:

```bash
docker compose up -d
```

Run the Spring Boot server:

```bash
mvn -pl sherwood-server spring-boot:run
# Server starts on http://localhost:12080
```

Or run the CLI:

```bash
java -jar sherwood-cli/target/sherwood-cli-*.jar [options]
```

## Features

### Reachability Analysis

Sherwood analyzes whether security findings are reachable from application entry points (main methods, public APIs, etc.):

```
Entry Point: com.example.App.main(String[])
Path to Vulnerability:
  main() : L5 →
    processRequest() : L9 (invoked at L6) →
      executeLogic() : L18 (invoked at L11) →
        vulnerableMethod() : L22 (invoked at L19)
```

### Visualization

Generate call graphs in multiple formats:

- **DOT (Graphviz)**: For rendering with `dot` command
- **Mermaid**: For embedding in Markdown/documentation
- **Text**: Human-readable text format

### Confidence Scoring

Each finding receives a confidence score based on reachability:
- **1.0**: Reachable from public entry point
- **0.8**: Reachable from instance initializer
- **0.5**: Internal method (not directly reachable)
- **0.0**: Isolated/unreachable code

## Configuration

Configuration is managed via `application.yml`:

```yaml
server:
  port: 12080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sherwood_db
  flyway:
    sql-migration-prefix: T
    out-of-order: true
```

Database migrations are in `sherwood-server/src/main/resources/db/migration/`

## Development

### Code Formatting

Formatting is enforced via `com.spotify.fmt:fmt-maven-plugin` and runs automatically during build:

```bash
# Check formatting
mvn com.spotify.fmt:fmt-maven-plugin:check

# Auto-fix formatting
mvn com.spotify.fmt:fmt-maven-plugin:format
```

### Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn -Dtest=JavaLanguageParserTest test
```

### Project Structure

```
sherwood/
├── sarif-model/        # Generated SARIF schema models
│   └── schema/         # SARIF 2.1.0 JSON schema
├── parsers/            # Language parsers and call graph analysis
│   └── grammers/java/  # ANTLR4 Java 20 grammar
├── sherwood-server/    # Spring Boot REST API
│   └── src/main/resources/db/migration/  # Flyway migrations
└── sherwood-cli/       # Command-line interface
```

## License

This project uses:
- SARIF Schema 2.1.0 (generated via jsonschema2pojo)
- ANTLR4 for Java parsing
- Spring Boot 3.5.x
- PostgreSQL with pgvector extension

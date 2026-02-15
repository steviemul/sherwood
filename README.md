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

Sherwood is a multi-module project consisting of:

**Backend (Maven, Java 21):**
- **`sarif-model`**: Auto-generated Java POJOs from SARIF 2.1.0 JSON schema
- **`parsers`**: Language parsers using ANTLR4 for call graph analysis and reachability detection
- **`sherwood-server`**: Spring Boot (WebFlux) REST API for analysis services
- **`sherwood-cli`**: Command-line interface for processing SARIF files

**Frontend:**
- **`sherwood-frontend`**: React 19 + TypeScript + Vite web interface with Material-UI for visualization and analysis

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.6+
- Node.js 18+ & npm (for frontend development)
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

Start all required services (PostgreSQL, Ollama, MinIO, Caddy):

```bash
docker compose up -d
```

Run the Spring Boot server:

```bash
mvn -pl sherwood-server spring-boot:run
# Backend API: http://localhost:12080
# JobRunr Dashboard: http://localhost:13080
```

Run the frontend (development mode):

```bash
cd sherwood-frontend
npm install
npm run dev
# Frontend: http://localhost:14080
```

Or run the CLI:

```bash
java -jar sherwood-cli/target/sherwood-cli-*.jar [options]
```

### Runtime Dependencies

The `docker-compose.yml` provides all required services:

- **PostgreSQL** with **pgvector** extension (port 5432) - data persistence and Spring AI vector store
- **Ollama** (port 11434) - AI-powered analysis via Spring AI
- **MinIO** (ports 9000/9001) - S3-compatible object storage for SARIF files
- **Caddy** (port 14080) - reverse proxy serving frontend and proxying API requests

Services are configured to:
- **Backend API**: `http://localhost:12080`
- **JobRunr Dashboard**: `http://localhost:13080` - background job monitoring
- **Frontend**: `http://localhost:14080` - served by Caddy
- **MinIO Console**: `http://localhost:9001` - S3 storage admin
- **PostgreSQL**: `jdbc:postgresql://localhost:5432/sherwood_db` (user: `postgres`, password: `password`)

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

storage:
  s3:
    endpoint: http://localhost:9000  # MinIO
    sarif-bucket: sarif

jobrunr:
  dashboard:
    enabled: true
    port: 13080
```

Database migrations are in `sherwood-server/src/main/resources/db/migration/` using the `T` prefix (e.g., `T2026.02.15.15.00__description.sql`)

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
├── sherwood-cli/       # Command-line interface
├── sherwood-frontend/  # React + TypeScript web UI
│   ├── src/            # React components and pages
│   ├── Caddyfile       # Caddy reverse proxy config
│   └── dist/           # Production build output
├── sarifs/             # Sample SARIF files for testing
└── http-requests/      # HTTP client request files
```

## License

This project uses:
- SARIF Schema 2.1.0 (generated via jsonschema2pojo)
- ANTLR4 for Java parsing
- Spring Boot 3.5.10
- React 19 with TypeScript and Vite
- PostgreSQL with pgvector extension
- MinIO for S3-compatible storage
- Ollama for local LLM integration

# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/io/github/yuokada/quarkus/`: Application Java code (REST resources, etc.).
- `src/main/resources/`: Configuration files including `application.properties`.
- `src/main/docker/`: Dockerfiles for JVM/native builds.
- `src/test/java/io/github/yuokada/quarkus/`: Test code (example: `GreetingResourceTest`).
- `spec/`: Specifications and design notes (example: `spec/v2.md`).

## Build, Test, and Development Commands
- `./mvnw quarkus:dev`: Run in dev mode (hot reload, Dev UI at `http://localhost:8080/q/dev/`).
- `./mvnw test`: Run unit tests (`*Test`).
- `./mvnw verify`: Build with integration tests (`*IT`, set `-DskipITs=false` if needed).
- `./mvnw package`: Build JARs (`target/quarkus-app/`).
- `./mvnw package -Dnative`: Native build (requires GraalVM).

## Coding Style & Naming Conventions
- Java 17 is required. Use 4-space indentation.
- Class names use PascalCase; methods/variables use camelCase.
- Keep REST resources named `*Resource`.
- No formatter/linter is configured; follow existing style.

## Testing Guidelines
- Tests use JUnit 5 (`quarkus-junit`) and RestAssured.
- Naming: unit tests are `*Test`, integration tests are `*IT`.
- Integration tests are skipped by default; use `-DskipITs=false` when needed.

## Commit & Pull Request Guidelines
- Commit conventions are not yet established.
- Recommendation: one change per commit with a clear summary (example: `Add greeting endpoint`).
- PRs should include a summary, verification steps, and screenshots/issues when relevant.

## Configuration Tips
- Primary settings live in `src/main/resources/application.properties`.
- Consider profile-specific settings (example: `application-dev.properties`).

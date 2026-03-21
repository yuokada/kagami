# GitHub Copilot Instructions

Follow these repository instructions when working in this project.

## General guidance

- Keep changes focused and consistent with the current shadow-proxy design.
- Write new or updated repository instructions, comments, and documentation in English.
- Avoid machine-specific paths, local-only assumptions, and committed secrets.
- Preserve clear separation between request handling, upstream dispatch, diff comparison, and reporting.
- Keep HTTP proxy behavior stable unless the task explicitly changes external behavior.

## Project context

- Main application code lives under `src/main/java/io/github/yuokada/quarkus/proxy/`.
- Configuration is primarily managed in `src/main/resources/application.properties`.
- Tests live under `src/test/java/io/github/yuokada/quarkus/proxy/`.
- Additional design notes live under `spec/`. (Note: some existing documents are in Japanese)

## Validation

- Prefer `./mvnw test` for logic changes.
- Prefer `./mvnw verify` when a change may affect packaging or broader runtime behavior.
- Clearly distinguish between checks you ran and checks you did not run.

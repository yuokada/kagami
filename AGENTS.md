# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/io/github/yuokada/quarkus/`: アプリ本体のJavaコード（RESTリソースなど）。
- `src/main/resources/`: `application.properties` を含む設定ファイル。
- `src/main/docker/`: JVM/ネイティブ向けのDockerfile群。
- `src/test/java/io/github/yuokada/quarkus/`: テストコード（例: `GreetingResourceTest`）。
- `spec/`: 仕様や設計メモ（例: `spec/v2.md`）。

## Build, Test, and Development Commands
- `./mvnw quarkus:dev`: 開発モード起動（ホットリロード、Dev UIは `http://localhost:8080/q/dev/`）。
- `./mvnw test`: ユニットテスト実行（`*Test`）。
- `./mvnw verify`: 統合テスト含むビルド（`*IT`、必要に応じて `-DskipITs=false`）。
- `./mvnw package`: JARを生成（`target/quarkus-app/`）。
- `./mvnw package -Dnative`: ネイティブビルド（GraalVM必要）。

## Coding Style & Naming Conventions
- Java 17前提。インデントはスペース4つ。
- クラス名はPascalCase、メソッド/変数はcamelCase。
- RESTリソースは `*Resource` 命名を維持。
- 自動フォーマッタ/リンタは現時点で未設定のため、既存のスタイルに合わせる。

## Testing Guidelines
- テストはJUnit 5（`quarkus-junit5`）とRestAssuredを使用。
- 命名: ユニットテストは `*Test`、統合テストは `*IT`。
- 統合テストはデフォルトでスキップされるため、必要時は `-DskipITs=false`。

## Commit & Pull Request Guidelines
- Git履歴がまだないため、コミット規約は未確立。
- 推奨: 1変更1コミット、明確な要約（例: `Add greeting endpoint`）。
- PRには変更概要、動作確認手順、必要ならスクリーンショットや関連Issueを記載。

## Configuration Tips
- 主要な設定は `src/main/resources/application.properties` に集約。
- 環境依存設定はプロファイル分離（例: `application-dev.properties`）を検討。

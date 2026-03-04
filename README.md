# kagami

Shadow Proxy (kagami) は既存 API (master) と開発中 API (shadow) のレスポンス差分を検出する開発者向けツールです。

## 使い方

- Proxy は master レスポンスをクライアントへ返し、差分は stdout/ログへ JSON Lines で出力します。
- `X-Shadow` ヘッダーがあるリクエストは shadow 送信をスキップします。

## 主要設定

`src/main/resources/application.properties` に以下の設定があります。

```
proxy.upstream.master=http://existing-api:8080
proxy.upstream.shadow=http://dev-api:8080
proxy.timeout.master=60s
proxy.timeout.shadow=60s
proxy.body.max-bytes=10485760
proxy.compare.enabled=true
proxy.compare.array-order-sensitive=true
proxy.compare.null-vs-missing-equal=false
proxy.compare.ignore-paths=$.timestamp,$.traceId
proxy.request-id.header=X-Request-Id
proxy.request-id.type=UUID
proxy.reporter.mode=stdout
```

設定のポイント:

- `proxy.compare.ignore-paths` は JSONPath で動的フィールドを除外します。
- `proxy.request-id.type` は `UUID` / `ULID` を選べます。
- `proxy.reporter.mode` は `stdout` / `logger` を選べます。

## ローカル実行

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/kagami-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

## テスト

```shell script
./mvnw test
```

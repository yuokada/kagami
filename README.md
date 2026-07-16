# kagami

Shadow Proxy (kagami) is a developer tool that detects response differences between existing APIs (master) and in-development APIs (shadow).

## Usage

- The proxy returns the master response to clients and emits JSON Lines diff reports to stdout/logs.
- Requests with the `X-Shadow` header skip the shadow upstream.

## Key Configuration

Place your settings in an external file (e.g. `config/application.properties`) and pass it at startup — see [Run Locally](#run-locally) below.
The default bundled config is at `src/main/resources/application.properties`.

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

Notes:

- `proxy.compare.ignore-paths` removes dynamic fields via JSONPath.
- `proxy.request-id.type` supports `UUID` / `ULID`.
- `proxy.reporter.mode` supports `stdout` / `logger`.

## Run Locally

Pass an external config file via `-Dquarkus.config.locations` to keep your settings outside the packaged artifact:

```shell script
# copy and edit the template
cp config/kagami.properties.example config/application.properties
```

## Running the application in dev mode

```shell script
./mvnw -Dquarkus.config.locations=config/application.properties quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

The project is configured with `quarkus.package.jar.type=uber-jar`, so this produces
a self-contained `target/kagami-1.0.0-SNAPSHOT-runner.jar`.

The application is now runnable using:

```shell script
java -Dquarkus.config.locations=config/application.properties -jar target/*-runner.jar
```

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

## Known Limitations

### HTTP/2 upstream support

When the upstream server communicates over HTTP/2, the Java `HttpClient` includes HTTP/2 pseudo-headers (`:status`, `:path`, etc.) in the response header map.
kagami strips these pseudo-headers (any header name starting with `:`) before forwarding the master response to the client.
Without this, Vert.x rejects them with `IllegalArgumentException: a header name cannot contain some prohibited characters, such as : :status` when building the HTTP/1.1 response.

In addition, hop-by-hop headers and headers named by `Connection` are removed on forwarding.
`content-length` is regenerated for regular responses and preserved for `HEAD` responses.

### Empty / no-body responses

Upstreams that return no body (e.g. `204 No Content`, `304 Not Modified`, connection errors) produce an empty `rawBody`.
kagami skips setting a JAX-RS entity in that case to avoid a `500` error from the Vert.x/RESTEasy serializer attempting to write an empty `byte[]`.
The `Content-Type` header is also suppressed when there is no body, for the same reason.

When the shadow upstream also returns an empty body, the comparator short-circuits and reports `SAME` rather than attempting JSON parsing — which would produce a `NullPointerException` (Jackson's `readTree("")` returns `null`) and cause a `500` response.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

## Tests

```shell script
./mvnw test
```

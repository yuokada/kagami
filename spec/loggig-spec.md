# Shadow Proxy Logging Specification (kagami)
Version: 1.1
Target: Quarkus-based Shadow Proxy (kagami)
Log pipeline: JSON → stdout → (Fluentd → Treasure Data)

---

# 1. Purpose

This document defines the logging specification for the kagami shadow proxy.

kagami is a long-running service that forwards every incoming request to both a master upstream and a shadow upstream in parallel, then compares the responses.
Therefore the logging model is **event-oriented**: one log record is emitted per lifecycle event within a single proxy request.

The goal of this specification is to ensure that logs are:

- Machine-readable (JSON)
- Correlatable by request via `requestId`
- Safe for large-scale log ingestion

---

# 2. Logging Architecture

```
kagami (Quarkus)
│
│ JSON logs (stdout)
▼
Fluentd / fluent-package
│
▼
Treasure Data
```

### Responsibilities

| Component | Responsibility |
|---|---|
| Quarkus Application | Generate structured logs |
| Fluentd | Enrich logs with environment metadata |
| Treasure Data | Storage and analytics |

Important principle:

> Application logs contain **business context only**.

Infrastructure metadata such as `service`, `environment`, `region`, `cluster` is injected by Fluentd filters.

---

# 3. Fluentd Enrichment

Fluentd uses the `filter` stage to enrich records.

Typical configuration:

```conf
<filter kagami.**>
  @type record_transformer
  <record>
    service kagami
    env prod
    cluster proxy-cluster-a
    region ap-northeast-1
    log_source fluentd
  </record>
</filter>
```

This keeps the application independent of deployment configuration.

---

# 4. Quarkus Extension

JSON logging is provided by the **`quarkus-logging-json`** extension.

- Extension guide: https://quarkus.io/guides/logging#json-logging
- Extension listing: https://quarkus.io/extensions/io.quarkus/quarkus-logging-json/

`pom.xml`:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-logging-json</artifactId>
</dependency>
```

`application.properties`:

```properties
quarkus.log.console.json.enabled=true
quarkus.log.console.json.pretty-print=false

# Human-readable output in dev mode
%dev.quarkus.log.console.json.pretty-print=true
```

Pretty printing is disabled in production because log pipelines require **one JSON record per line**.

---

# 5. Logging Model

kagami logging is **event-driven**.

The primary entity is:

```
proxy request event
```

Each request produces two events:

| # | Event | Timing |
|---|---|---|
| 1 | `request.started` | Immediately after request is received |
| 2 | `request.succeeded` or `request.failed` | After master response is received |

---

# 6. Required Log Fields

These fields are set via MDC and appear in every log record automatically.

| Field | MDC Key (`LoggingKeys`) | Description |
|---|---|---|
| `requestId` | `REQUEST_ID` | Proxy-generated or client-supplied request ID |
| `method` | `METHOD` | HTTP method (GET, POST, …) |
| `path` | `PATH` | Request path |
| `eventType` | `EVENT_TYPE` | Type of proxy event (see §8) |
| `status` | `STATUS` | `success` / `failed` |

Standard Quarkus JSON logging fields (`time`, `level`, `message`, `logger`) are added automatically by the `quarkus-logging-json` extension.

---

# 7. Recommended Fields

These fields are added at the end of the request lifecycle.

| Field | MDC Key (`LoggingKeys`) | Description |
|---|---|---|
| `durationMs` | `DURATION_MS` | Total proxy request duration in milliseconds |
| `httpStatus` | `HTTP_STATUS` | HTTP status code returned by the master upstream |

---

# 8. MDC Usage

MDC (Mapped Diagnostic Context) attaches contextual fields to every log record emitted on the same thread.

### Lifecycle

```java
// On request entry
LoggingContext.startRequest(requestId, method, path);
// → puts requestId, method, path into MDC

// On request exit (in finally block)
LoggingContext.clear();
// → clears all MDC entries
```

### Per-event MDC writes

```java
MDC.put(LoggingKeys.EVENT_TYPE, "request.started");
LOGGER.info("request started");

// ... dispatch to upstreams ...

MDC.put(LoggingKeys.DURATION_MS, String.valueOf(durationMs));
MDC.put(LoggingKeys.HTTP_STATUS, String.valueOf(masterResponse.status()));
MDC.put(LoggingKeys.EVENT_TYPE, "request.succeeded");
MDC.put(LoggingKeys.STATUS, "success");
LOGGER.info("request succeeded");
```

> **Note on async threads**: MDC is thread-local. Async callbacks in `UpstreamClient` run on a different thread pool and therefore do **not** inherit MDC automatically. MDC propagation into async handlers is out of scope for this version.

---

# 9. Event Types

| Event | Logger level | Description |
|---|---|---|
| `request.started` | INFO | Proxy request received |
| `request.succeeded` | INFO | Master upstream responded without error or timeout |
| `request.failed` | WARN | Master upstream timed out or returned an error |

---

# 10. Error Logging

When `masterResponse.timedOut()` or `masterResponse.error()` is true, the request is logged as failed:

```java
MDC.put(LoggingKeys.EVENT_TYPE, "request.failed");
MDC.put(LoggingKeys.STATUS, "failed");
LOGGER.warn("request failed");
```

Large payloads (request/response bodies) must **not** be logged directly.

---

# 11. Implementation Structure

```
src/main/java/io/github/yuokada/quarkus/proxy/logging/
 ├── LoggingKeys.java      # MDC key constants
 └── LoggingContext.java   # MDC lifecycle utilities
```

`LoggingKeys.java` — string constants, no logic.
`LoggingContext.java` — `startRequest(requestId, method, path)` and `clear()`.

Callers (`ProxyResource`) use these utilities directly; modules must not construct MDC key strings by hand.

---

# 12. Example Log Records

### request.started

```json
{
  "timestamp": "2026-03-13T10:00:00.000Z",
  "level": "INFO",
  "message": "request started",
  "loggerName": "io.github.yuokada.quarkus.proxy.ProxyResource",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "method": "GET",
  "path": "/api/users/42",
  "eventType": "request.started"
}
```

### request.succeeded

```json
{
  "timestamp": "2026-03-13T10:00:00.184Z",
  "level": "INFO",
  "message": "request succeeded",
  "loggerName": "io.github.yuokada.quarkus.proxy.ProxyResource",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "method": "GET",
  "path": "/api/users/42",
  "eventType": "request.succeeded",
  "status": "success",
  "httpStatus": "200",
  "durationMs": "184"
}
```

### request.failed

```json
{
  "timestamp": "2026-03-13T10:00:60.001Z",
  "level": "WARN",
  "message": "request failed",
  "loggerName": "io.github.yuokada.quarkus.proxy.ProxyResource",
  "requestId": "550e8400-e29b-41d4-a716-446655440001",
  "method": "POST",
  "path": "/api/orders",
  "eventType": "request.failed",
  "status": "failed",
  "httpStatus": "0",
  "durationMs": "60001"
}
```

---

# 13. Log Query Examples (Treasure Data)

### Error rate by HTTP status

```sql
SELECT
  httpStatus,
  count(*)
FROM kagami_logs
WHERE TD_TIME_RANGE(time, '2026-03-13', NULL)
GROUP BY httpStatus
```

### Failed requests

```sql
SELECT
  requestId,
  method,
  path,
  durationMs
FROM kagami_logs
WHERE status = 'failed'
```

### p99 latency by path

```sql
SELECT
  path,
  approx_percentile(CAST(durationMs AS double), 0.99) AS p99_ms
FROM kagami_logs
WHERE eventType = 'request.succeeded'
GROUP BY path
```

---

# 14. Non-Functional Requirements

| Requirement | Value |
|---|---|
| Log format | JSON |
| Output | stdout |
| Encoding | UTF-8 |
| Pretty print | Disabled (production) |
| Record format | One JSON object per line |

---

# 15. Summary

kagami logging is designed around **event-based observability** at the proxy request level.

Key principles:

1. Logs must be structured (JSON) via `quarkus-logging-json`
2. Every request is correlated via `requestId` using MDC
3. Two events are emitted per request: `request.started` and `request.succeeded`/`request.failed`
4. Application logs contain business context only; deployment metadata is injected by Fluentd
5. Request/response body payloads must never be logged

# Crawler Logging Specification
Version: 1.0  
Target: Quarkus-based Crawler  
Log pipeline: JSON → Fluentd → Treasure Data

---

# 1. Purpose

This document defines the logging specification for crawler applications.

Crawler systems typically run as long-lived services and process a large number of URLs.  
Therefore the logging model is **event-oriented**, not execution-oriented.

The goal of this specification is to ensure that logs are:

- Machine-readable (JSON)
- Searchable in Treasure Data
- Correlatable by crawl session and request
- Safe for large-scale log ingestion

---

# 2. Logging Architecture

```

Quarkus Crawler
│
│ JSON logs (stdout)
▼
Fluentd / fluent-package
│
▼
Treasure Data

````

### Responsibilities

| Component | Responsibility |
|---|---|
| Quarkus Application | Generate structured logs |
| Fluentd | Enrich logs with environment metadata |
| Treasure Data | Storage and analytics |

Important principle:

> Application logs contain **business context only**.

Infrastructure metadata such as:

- service
- environment
- region
- cluster

is injected by Fluentd filters.

---

# 3. Fluentd Enrichment

Fluentd uses the `filter` stage to enrich records.

Typical configuration:

```conf
<filter crawler.**>
  @type record_transformer
  <record>
    service crawler
    env prod
    cluster crawler-cluster-a
    region ap-northeast-1
    log_source fluentd
  </record>
</filter>
````

This keeps the application independent of deployment configuration.

---

# 4. Quarkus Logging Configuration

Example configuration:

```properties
quarkus.log.console.json.enabled=true
quarkus.log.console.json.pretty-print=false
```

Pretty printing is disabled because log pipelines require **one JSON record per line**.

---

# 5. Logging Model

Crawler logging is **event-driven**.

The primary entity is:

```
URL request event
```

Each event corresponds to:

* URL fetch
* Parsing
* Scheduling
* Error
* Retry

---

# 6. Required Log Fields

| Field     | Description              |
| --------- | ------------------------ |
| time      | Unix timestamp           |
| level     | Log level                |
| message   | Human readable message   |
| logger    | Logger class             |
| crawlId   | Crawl session identifier |
| requestId | Request identifier       |
| url       | Target URL               |
| eventType | Type of crawler event    |
| status    | success / failed         |

---

# 7. Recommended Fields

| Field      | Description          |
| ---------- | -------------------- |
| method     | HTTP method          |
| host       | Target host          |
| attempt    | Retry count          |
| durationMs | Request duration     |
| httpStatus | HTTP status code     |
| depth      | Crawl depth          |
| parentUrl  | Referrer URL         |
| workerId   | Worker identifier    |
| errorType  | Error classification |

---

# 8. MDC Usage

MDC (Mapped Diagnostic Context) attaches contextual fields to logs automatically.

Typical MDC fields:

| MDC Key   | Purpose          |
| --------- | ---------------- |
| crawlId   | Crawl session    |
| requestId | Request tracking |
| url       | Target URL       |
| attempt   | Retry counter    |
| depth     | Crawl depth      |

Example:

```java
MDC.put("crawlId", crawlId);
MDC.put("requestId", requestId);
MDC.put("url", url);
```

---

# 9. Event Types

Standard event types:

| Event             | Description            |
| ----------------- | ---------------------- |
| crawl.started     | Crawl session started  |
| request.started   | HTTP request initiated |
| request.succeeded | Request completed      |
| request.failed    | Request failed         |
| parse.started     | Parsing started        |
| parse.succeeded   | Parsing succeeded      |
| parse.failed      | Parsing failed         |
| url.enqueued      | URL scheduled          |
| url.skipped       | URL skipped            |
| rate_limited      | Rate limit event       |
| crawl.finished    | Crawl completed        |

Note:

For long-running crawlers, `crawl.finished` may not always exist.

---

# 10. Error Logging

Error logs must include:

| Field          | Description               |
| -------------- | ------------------------- |
| status         | failed                    |
| errorType      | Error category            |
| exceptionClass | Java exception            |
| retryable      | Whether retry is possible |
| attempt        | Retry attempt count       |

Large data such as:

* HTML body
* Payloads
* SQL queries

must **not** be logged directly.

Instead, reference identifiers should be used.

---

# 11. Example Log Record

```json
{
  "time":1773360000,
  "level":"INFO",
  "message":"request succeeded",
  "logger":"com.example.crawler.FetchService",
  "crawlId":"crawl-20260313-01",
  "requestId":"req-8f3a",
  "url":"https://example.com/a",
  "eventType":"request.succeeded",
  "status":"success",
  "method":"GET",
  "attempt":1,
  "depth":2,
  "httpStatus":200,
  "durationMs":184
}
```

---

# 12. Log Query Examples (Treasure Data)

### Error rate

```sql
SELECT
  httpStatus,
  count(*)
FROM crawler_logs
WHERE crawlId = 'crawl-20260313-01'
GROUP BY httpStatus
```

### Failed URLs

```sql
SELECT
  url,
  errorType
FROM crawler_logs
WHERE status='failed'
```

### Crawl progress

```sql
SELECT
  eventType,
  count(*)
FROM crawler_logs
WHERE crawlId='crawl-20260313-01'
GROUP BY eventType
```

---

# 13. Non-Functional Requirements

| Requirement   | Description              |
| ------------- | ------------------------ |
| Log format    | JSON                     |
| Output        | stdout                   |
| Encoding      | UTF-8                    |
| Pretty Print  | Disabled                 |
| Record Format | One JSON object per line |

---

# 14. Implementation Guidelines

Crawler modules should not manipulate logging details directly.

Instead use a shared logging utility:

```
logging/
 ├ LoggingContext.java
 ├ LoggingKeys.java
 └ LoggingUtils.java
```

Example:

```java
LoggingContext.startRequest(crawlId, requestId, url);
LoggingContext.finishRequest(status, duration);
```

This ensures consistent log structure across the crawler.

---

# 15. Summary

Crawler logging is designed around **event-based observability**.

Key principles:

1. Logs must be structured (JSON)
2. Logs must be correlated via `crawlId` and `requestId`
3. Application logs contain business context only
4. Deployment metadata is injected by Fluentd
5. Large payloads must never be logged

This design enables scalable analytics using Treasure Data while keeping crawler services observable and debuggable.
package io.github.yuokada.quarkus.proxy;

import java.util.List;
import java.util.Map;

public record RequestContext(
        String requestId,
        String clientRequestId,
        String method,
        String path,
        String query,
        Map<String, List<String>> headers,
        long startTimeNanos) {}

package io.github.yuokada.quarkus.proxy.model;

import java.util.List;
import java.util.Map;

public record UpstreamResponse(
        int status,
        Map<String, List<String>> headers,
        byte[] rawBody,
        byte[] decodedBody,
        long latencyMs,
        boolean timedOut,
        boolean tooLarge,
        boolean error,
        String errorMessage) {}

package io.github.yuokada.quarkus.proxy;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class HttpHeaderSanitizer {
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade");
    private static final Set<String> RESTRICTED_REQUEST_HEADERS = Set.of(
            "content-length",
            "expect",
            "host");

    private HttpHeaderSanitizer() {}

    static Map<String, List<String>> sanitizeRequest(
            Map<String, List<String>> headers, String requestIdHeader) {
        Set<String> excluded = excludedHeaders(headers);
        excluded.addAll(RESTRICTED_REQUEST_HEADERS);
        excluded.add(requestIdHeader.toLowerCase(Locale.ROOT));
        return copyWithout(headers, excluded, false);
    }

    static Map<String, List<String>> sanitizeResponse(Map<String, List<String>> headers) {
        return copyWithout(headers, excludedHeaders(headers), true);
    }

    private static Set<String> excludedHeaders(Map<String, List<String>> headers) {
        Set<String> excluded = new HashSet<>(HOP_BY_HOP_HEADERS);
        headers.forEach((key, values) -> {
            if ("connection".equalsIgnoreCase(key)) {
                values.stream()
                        .flatMap(value -> List.of(value.split(",")).stream())
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .forEach(excluded::add);
            }
        });
        return excluded;
    }

    private static Map<String, List<String>> copyWithout(
            Map<String, List<String>> headers, Set<String> excluded, boolean excludePseudoHeaders) {
        Map<String, List<String>> sanitized = new LinkedHashMap<>();
        headers.forEach((key, values) -> {
            String normalized = key.toLowerCase(Locale.ROOT);
            if ((!excludePseudoHeaders || !key.startsWith(":")) && !excluded.contains(normalized)) {
                sanitized.put(key, List.copyOf(values));
            }
        });
        return sanitized;
    }
}

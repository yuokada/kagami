package io.github.yuokada.quarkus.proxy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpHeaderSanitizerTest {

    @Test
    void requestRemovesHopByHopRestrictedAndConnectionNamedHeaders() {
        Map<String, List<String>> sanitized = HttpHeaderSanitizer.sanitizeRequest(
                Map.of(
                        "Authorization", List.of("Bearer token"),
                        "Connection", List.of("keep-alive, X-Remove-Me"),
                        "Content-Length", List.of("12"),
                        "Expect", List.of("100-continue"),
                        "Host", List.of("proxy.example"),
                        "TE", List.of("trailers"),
                        "X-Remove-Me", List.of("value"),
                        "X-Request-Id", List.of("client-id")),
                "X-Request-Id");

        assertEquals(Map.of("Authorization", List.of("Bearer token")), sanitized);
    }

    @Test
    void responseRemovesHopByHopPseudoAndConnectionNamedHeaders() {
        Map<String, List<String>> sanitized = HttpHeaderSanitizer.sanitizeResponse(Map.of(
                ":status", List.of("200"),
                "Connection", List.of("X-Internal"),
                "Content-Type", List.of("application/json"),
                "Keep-Alive", List.of("timeout=5"),
                "X-Internal", List.of("secret")));

        assertTrue(sanitized.containsKey("Content-Type"));
        assertFalse(sanitized.containsKey(":status"));
        assertFalse(sanitized.containsKey("Connection"));
        assertFalse(sanitized.containsKey("Keep-Alive"));
        assertFalse(sanitized.containsKey("X-Internal"));
    }
}

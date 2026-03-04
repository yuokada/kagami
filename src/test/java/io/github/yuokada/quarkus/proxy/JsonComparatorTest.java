package io.github.yuokada.quarkus.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yuokada.quarkus.proxy.model.ComparisonResult;
import io.github.yuokada.quarkus.proxy.model.UpstreamResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonComparatorTest {

    private JsonComparator comparator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        JsonNormalizer normalizer = new JsonNormalizer();
        normalizer.objectMapper = objectMapper;
        comparator = new JsonComparator();
        comparator.objectMapper = objectMapper;
        comparator.jsonNormalizer = normalizer;
    }

    @Test
    void compareRespectsIgnorePaths() {
        ProxyConfig.Compare compareConfig = new TestCompareConfig(true, true, false, List.of("$.timestamp"));
        ComparisonResult result = comparator.compare(
                response("{\"timestamp\":\"t1\",\"value\":1}"),
                response("{\"timestamp\":\"t2\",\"value\":1}"),
                compareConfig);

        assertEquals(ComparisonResult.Result.SAME, result.result());
    }

    @Test
    void compareDetectsArrayOrderDifference() {
        ProxyConfig.Compare compareConfig = new TestCompareConfig(true, true, false, List.of());
        ComparisonResult result = comparator.compare(
                response("{\"items\":[1,2,3]}"),
                response("{\"items\":[3,2,1]}"),
                compareConfig);

        assertEquals(ComparisonResult.Result.DIFF, result.result());
        assertTrue(result.diffEntries().size() >= 1);
    }

    @Test
    void compareTreatsNullAndMissingAsEqualWhenConfigured() {
        ProxyConfig.Compare compareConfig = new TestCompareConfig(true, true, true, List.of());
        ComparisonResult result = comparator.compare(
                response("{\"value\":null}"),
                response("{}"),
                compareConfig);

        assertEquals(ComparisonResult.Result.SAME, result.result());
    }

    @Test
    void compareTreatsArrayOrderInsensitiveWhenConfigured() {
        ProxyConfig.Compare compareConfig = new TestCompareConfig(true, false, false, List.of());
        ComparisonResult result = comparator.compare(
                response("{\"items\":[1,2,3]}"),
                response("{\"items\":[3,2,1]}"),
                compareConfig);

        assertEquals(ComparisonResult.Result.SAME, result.result());
    }

    @Test
    void compareDetectsStatusDiff() {
        ProxyConfig.Compare compareConfig = new TestCompareConfig(true, true, false, List.of());
        UpstreamResponse master = responseWithStatus("{\"value\":1}", 200);
        UpstreamResponse shadow = responseWithStatus("{\"value\":1}", 500);
        ComparisonResult result = comparator.compare(master, shadow, compareConfig);

        assertEquals(ComparisonResult.Result.DIFF, result.result());
        assertEquals("$.status", result.diffEntries().get(0).path());
    }

    private UpstreamResponse response(String json) {
        return responseWithStatus(json, 200);
    }

    private UpstreamResponse responseWithStatus(String json, int status) {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        return new UpstreamResponse(status, java.util.Map.of(), body, body, 0, false, false, false, null);
    }

    private record TestCompareConfig(
            boolean enabled,
            boolean arrayOrderSensitive,
            boolean nullVsMissingEqual,
            List<String> ignorePaths) implements ProxyConfig.Compare {}
}

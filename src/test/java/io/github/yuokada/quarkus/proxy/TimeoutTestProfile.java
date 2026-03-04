package io.github.yuokada.quarkus.proxy;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class TimeoutTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "proxy.timeout.master", "100ms",
                "proxy.timeout.shadow", "100ms");
    }
}

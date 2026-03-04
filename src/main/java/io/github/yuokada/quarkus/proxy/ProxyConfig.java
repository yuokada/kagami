package io.github.yuokada.quarkus.proxy;

import io.smallrye.config.ConfigMapping;
import java.time.Duration;
import java.util.List;

@ConfigMapping(prefix = "proxy")
public interface ProxyConfig {
    Upstream upstream();

    Timeout timeout();

    Body body();

    Compare compare();

    Concurrency concurrency();

    RequestId requestId();

    Reporter reporter();

    interface Upstream {
        String master();

        String shadow();
    }

    interface Timeout {
        Duration master();

        Duration shadow();
    }

    interface Body {
        long maxBytes();
    }

    interface Compare {
        boolean enabled();

        boolean arrayOrderSensitive();

        boolean nullVsMissingEqual();

        List<String> ignorePaths();
    }

    interface Concurrency {
        int maxComparisons();
    }

    interface RequestId {
        String header();

        String type();
    }

    interface Reporter {
        String mode();
    }
}

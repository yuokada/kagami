package io.github.yuokada.quarkus.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComparisonLimiterTest {

    @Test
    void limitsConcurrentComparisons() {
        ComparisonLimiter limiter = new ComparisonLimiter();
        limiter.proxyConfig = new TestProxyConfig(1);

        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
        limiter.release();
        assertTrue(limiter.tryAcquire());
    }

    private record TestProxyConfig(int maxComparisons) implements ProxyConfig {
        @Override
        public Upstream upstream() {
            return null;
        }

        @Override
        public Timeout timeout() {
            return null;
        }

        @Override
        public Body body() {
            return null;
        }

        @Override
        public Compare compare() {
            return null;
        }

        @Override
        public Concurrency concurrency() {
            return () -> maxComparisons;
        }

        @Override
        public RequestId requestId() {
            return null;
        }

        @Override
        public Reporter reporter() {
            return null;
        }
    }
}

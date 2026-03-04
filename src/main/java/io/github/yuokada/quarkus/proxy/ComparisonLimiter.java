package io.github.yuokada.quarkus.proxy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.Semaphore;

@ApplicationScoped
public class ComparisonLimiter {
    private Semaphore semaphore;

    @Inject
    ProxyConfig proxyConfig;

    void init() {
        if (semaphore == null) {
            semaphore = new Semaphore(proxyConfig.concurrency().maxComparisons());
        }
    }

    public boolean tryAcquire() {
        init();
        return semaphore.tryAcquire();
    }

    public void release() {
        if (semaphore != null) {
            semaphore.release();
        }
    }
}

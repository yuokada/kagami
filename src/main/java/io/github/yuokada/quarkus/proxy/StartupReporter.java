package io.github.yuokada.quarkus.proxy;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StartupReporter {
    private static final Logger LOGGER = Logger.getLogger(StartupReporter.class);

    @Inject
    ProxyConfig proxyConfig;

    void onStart(@Observes StartupEvent event) {
        LOGGER.infof(
                "Shadow Proxy upstreams: master=%s shadow=%s",
                proxyConfig.upstream().master(),
                proxyConfig.upstream().shadow());
    }
}

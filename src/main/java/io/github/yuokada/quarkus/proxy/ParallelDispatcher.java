package io.github.yuokada.quarkus.proxy;

import io.github.yuokada.quarkus.proxy.model.UpstreamPair;
import io.github.yuokada.quarkus.proxy.model.UpstreamResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class ParallelDispatcher {

    @Inject
    ProxyConfig proxyConfig;

    @Inject
    UpstreamClient upstreamClient;

    public UpstreamPair dispatch(RequestContext context, UriTargets targets, byte[] body, boolean sendShadow) {
        CompletableFuture<UpstreamResponse> masterFuture = upstreamClient.send(
                targets.master(),
                context.method(),
                context.headers(),
                body,
                proxyConfig.timeout().master());

        CompletableFuture<UpstreamResponse> shadowFuture = sendShadow
                ? upstreamClient.send(
                        targets.shadow(),
                        context.method(),
                        withShadowHeader(context.headers()),
                        body,
                        proxyConfig.timeout().shadow())
                : CompletableFuture.completedFuture(null);

        return new UpstreamPair(masterFuture.join(), shadowFuture.join());
    }

    private Map<String, List<String>> withShadowHeader(Map<String, List<String>> headers) {
        Map<String, List<String>> updated = new java.util.HashMap<>(headers);
        updated.put("X-Shadow", List.of("true"));
        return updated;
    }

    public record UriTargets(URI master, URI shadow) {}
}

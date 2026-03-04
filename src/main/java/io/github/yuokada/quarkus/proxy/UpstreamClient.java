package io.github.yuokada.quarkus.proxy;

import io.github.yuokada.quarkus.proxy.model.UpstreamResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

@ApplicationScoped
public class UpstreamClient {
    private final HttpClient httpClient;

    @Inject
    ProxyConfig proxyConfig;

    public UpstreamClient() {
        this.httpClient = HttpClient.newBuilder().build();
    }

    public CompletableFuture<UpstreamResponse> send(
            URI uri,
            String method,
            Map<String, List<String>> headers,
            byte[] body,
            Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri).timeout(timeout);
        headers.forEach((key, values) -> values.forEach(value -> builder.header(key, value)));
        if (body != null && body.length > 0 && !method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("HEAD")) {
            builder.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        long start = System.nanoTime();
        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .handle((response, throwable) -> {
                    long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                    if (throwable != null) {
                        return new UpstreamResponse(0, Map.of(), new byte[0], new byte[0], latencyMs, true, false, true, throwable.getMessage());
                    }
                    byte[] rawBody = response.body();
                    byte[] decodedBody = decodeBody(response.headers().map(), rawBody);
                    boolean tooLarge = decodedBody.length > proxyConfig.body().maxBytes();
                    return new UpstreamResponse(
                            response.statusCode(),
                            response.headers().map(),
                            rawBody,
                            decodedBody,
                            latencyMs,
                            false,
                            tooLarge,
                            false,
                            null);
                });
    }

    private byte[] decodeBody(Map<String, List<String>> headers, byte[] body) {
        if (body == null) {
            return new byte[0];
        }
        String encoding = headerValue(headers, "content-encoding");
        if (encoding != null && encoding.toLowerCase(Locale.ROOT).contains("gzip")) {
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(body))) {
                return gzipInputStream.readAllBytes();
            } catch (IOException exception) {
                return body;
            }
        }
        return body;
    }

    private String headerValue(Map<String, List<String>> headers, String key) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst()
                .orElse(null);
    }
}

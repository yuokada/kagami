package io.github.yuokada.quarkus.proxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyUpstreamTestResource implements QuarkusTestResourceLifecycleManager {

    private static final AtomicInteger MASTER_COUNT = new AtomicInteger();
    private static final AtomicInteger SHADOW_COUNT = new AtomicInteger();

    private HttpServer masterServer;
    private HttpServer shadowServer;
    private ExecutorService masterExecutor;
    private ExecutorService shadowExecutor;

    @Override
    public Map<String, String> start() {
        try {
            masterServer = startServer(MASTER_COUNT, "{\"message\":\"master\"}");
            shadowServer = startServer(SHADOW_COUNT, "{\"message\":\"shadow\"}");
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }

        Map<String, String> config = new HashMap<>();
        config.put("proxy.upstream.master", urlFor(masterServer));
        config.put("proxy.upstream.shadow", urlFor(shadowServer));
        config.put("proxy.body.max-bytes", "32");
        return config;
    }

    @Override
    public void stop() {
        if (masterServer != null) {
            masterServer.stop(0);
        }
        if (shadowServer != null) {
            shadowServer.stop(0);
        }
        if (masterExecutor != null) {
            shutdownExecutor(masterExecutor);
        }
        if (shadowExecutor != null) {
            shutdownExecutor(shadowExecutor);
        }
    }

    public static int masterCount() {
        return MASTER_COUNT.get();
    }

    public static int shadowCount() {
        return SHADOW_COUNT.get();
    }

    public static void resetCounts() {
        MASTER_COUNT.set(0);
        SHADOW_COUNT.set(0);
    }

    private HttpServer startServer(AtomicInteger counter, String responseBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", new JsonHandler(counter, responseBody));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        server.setExecutor(executor);
        if (masterServer == null) {
            masterExecutor = executor;
        } else {
            shadowExecutor = executor;
        }
        server.start();
        return server;
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private String urlFor(HttpServer server) {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static class JsonHandler implements HttpHandler {
        private final AtomicInteger counter;
        private final byte[] responseBody;

        private JsonHandler(AtomicInteger counter, String responseBody) {
            this.counter = counter;
            this.responseBody = responseBody.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            counter.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            if (path.contains("/gzip")) {
                byte[] gzipped = gzip(responseBody);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.getResponseHeaders().add("Content-Encoding", "gzip");
                exchange.sendResponseHeaders(200, gzipped.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(gzipped);
                }
                return;
            }
            if (path.contains("/large")) {
                byte[] large = ("{\"message\":\"" + "x".repeat(64) + "\"}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, large.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(large);
                }
                return;
            }
            if (path.contains("/slow")) {
                sleep(1500);
            }
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBody.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        }

        private byte[] gzip(byte[] input) throws IOException {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            try (java.util.zip.GZIPOutputStream gzipStream = new java.util.zip.GZIPOutputStream(buffer)) {
                gzipStream.write(input);
            }
            return buffer.toByteArray();
        }

        private void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

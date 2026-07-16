package io.github.yuokada.quarkus.proxy;

import io.github.yuokada.quarkus.proxy.model.UpstreamResponse;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpstreamClientTest {

    @Test
    void timeoutIsReportedSeparatelyFromOtherFailures() {
        UpstreamResponse failure = UpstreamClient.failureResponse(
                new CompletionException(new HttpTimeoutException("timed out")), 25);

        assertTrue(failure.timedOut());
        assertFalse(failure.error());
        assertEquals(Response.Status.GATEWAY_TIMEOUT.getStatusCode(), new ProxyResource().clientStatus(failure));
    }

    @Test
    void connectionFailureIsAnErrorAndReturnsBadGateway() {
        UpstreamResponse failure = UpstreamClient.failureResponse(
                new CompletionException(new IOException("connection refused")), 25);

        assertFalse(failure.timedOut());
        assertTrue(failure.error());
        assertEquals("connection refused", failure.errorMessage());
        assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), new ProxyResource().clientStatus(failure));
    }

    @Test
    void upstreamStatusIsPreserved() {
        UpstreamResponse response = new UpstreamResponse(
                503, Map.of(), new byte[0], new byte[0], 1, false, false, false, null);

        assertEquals(503, new ProxyResource().clientStatus(response));
    }
}

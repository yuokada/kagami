package io.github.yuokada.quarkus.proxy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RequestReceiver {
    private static final String CLIENT_REQUEST_HEADER = "X-Client-Request-Id";
    @Inject
    ProxyConfig proxyConfig;

    @Inject
    RequestIdGenerator requestIdGenerator;

    public RequestContext receive(String method, UriInfo uriInfo, HttpHeaders headers) {
        String requestId = requestIdGenerator.generate();
        String requestIdHeader = proxyConfig.requestId().header();
        String clientRequestId = headers.getHeaderString(requestIdHeader);

        Map<String, List<String>> outgoingHeaders = new HashMap<>(
                HttpHeaderSanitizer.sanitizeRequest(headers.getRequestHeaders(), requestIdHeader));
        outgoingHeaders.put(requestIdHeader, List.of(requestId));
        if (clientRequestId != null && !clientRequestId.isBlank()) {
            outgoingHeaders.put(CLIENT_REQUEST_HEADER, List.of(clientRequestId));
        }

        return new RequestContext(
                requestId,
                clientRequestId,
                method,
                uriInfo.getPath(),
                uriInfo.getRequestUri().getRawQuery(),
                outgoingHeaders,
                System.nanoTime());
    }
}

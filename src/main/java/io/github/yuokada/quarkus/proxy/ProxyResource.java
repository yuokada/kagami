package io.github.yuokada.quarkus.proxy;

import io.github.yuokada.quarkus.proxy.model.ComparisonResult;
import io.github.yuokada.quarkus.proxy.model.DiffEntry;
import io.github.yuokada.quarkus.proxy.model.UpstreamPair;
import io.github.yuokada.quarkus.proxy.model.UpstreamResponse;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Path("/{path:.*}")
@Consumes("*/*")
@Produces("*/*")
public class ProxyResource {
    private static final String SHADOW_HEADER = "X-Shadow";

    @Inject
    ProxyConfig proxyConfig;

    @Inject
    RequestReceiver requestReceiver;

    @Inject
    ParallelDispatcher parallelDispatcher;

    @Inject
    ComparisonLimiter comparisonLimiter;

    @Inject
    JsonComparator jsonComparator;

    @Inject
    DiffReporter diffReporter;

    @PostConstruct
    void init() {
    }

    @GET
    public Response get(@PathParam("path") String path, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        return handle("GET", path, uriInfo, headers, null);
    }

    @HEAD
    public Response head(@PathParam("path") String path, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        return handle("HEAD", path, uriInfo, headers, null);
    }

    @OPTIONS
    public Response options(@PathParam("path") String path, @Context UriInfo uriInfo, @Context HttpHeaders headers) {
        return handle("OPTIONS", path, uriInfo, headers, null);
    }

    @DELETE
    public Response delete(@PathParam("path") String path, @Context UriInfo uriInfo, @Context HttpHeaders headers, byte[] body) {
        return handle("DELETE", path, uriInfo, headers, body);
    }

    @POST
    public Response post(@PathParam("path") String path, @Context UriInfo uriInfo, @Context HttpHeaders headers, byte[] body) {
        return handle("POST", path, uriInfo, headers, body);
    }

    @PUT
    public Response put(@PathParam("path") String path, @Context UriInfo uriInfo, @Context HttpHeaders headers, byte[] body) {
        return handle("PUT", path, uriInfo, headers, body);
    }

    @PATCH
    public Response patch(@PathParam("path") String path, @Context UriInfo uriInfo, @Context HttpHeaders headers, byte[] body) {
        return handle("PATCH", path, uriInfo, headers, body);
    }

    private Response handle(String method, String path, UriInfo uriInfo, HttpHeaders headers, byte[] body) {
        RequestContext context = requestReceiver.receive(method, uriInfo, headers);
        boolean sendShadow = headers.getHeaderString(SHADOW_HEADER) == null;
        ParallelDispatcher.UriTargets targets = new ParallelDispatcher.UriTargets(
                buildUpstreamUri(proxyConfig.upstream().master(), uriInfo),
                buildUpstreamUri(proxyConfig.upstream().shadow(), uriInfo));

        UpstreamPair responses = parallelDispatcher.dispatch(context, targets, body, sendShadow);
        UpstreamResponse masterResponse = responses.master();
        UpstreamResponse shadowResponse = responses.shadow();

        if (shadowResponse != null) {
            emitReport(context, masterResponse, shadowResponse);
        }

        return buildClientResponse(masterResponse);
    }

    private void emitReport(RequestContext context, UpstreamResponse master, UpstreamResponse shadow) {
        ComparisonResult comparison = compare(master, shadow);
        diffReporter.report(
                context.requestId(),
                context.method(),
                context.path(),
                master,
                shadow,
                comparison,
                comparison.diffEntries());
    }

    private ComparisonResult compare(UpstreamResponse master, UpstreamResponse shadow) {
        if (!comparisonLimiter.tryAcquire()) {
            return new ComparisonResult(ComparisonResult.Result.ERROR, List.of(), "comparison limit reached");
        }
        try {
            if (master.timedOut() || shadow.timedOut()) {
                return new ComparisonResult(ComparisonResult.Result.TIMEOUT, List.of(), null);
            }
            if (master.error() || shadow.error()) {
                String message = master.errorMessage() != null ? master.errorMessage() : shadow.errorMessage();
                return new ComparisonResult(ComparisonResult.Result.ERROR, List.of(), message);
            }
            if (master.tooLarge() || shadow.tooLarge()) {
                return new ComparisonResult(ComparisonResult.Result.TOO_LARGE, List.of(), null);
            }
            return jsonComparator.compare(master, shadow, proxyConfig.compare());
        } finally {
            comparisonLimiter.release();
        }
    }

    private Response buildClientResponse(UpstreamResponse masterResponse) {
        int status = masterResponse.status() == 0 ? Response.Status.GATEWAY_TIMEOUT.getStatusCode() : masterResponse.status();
        Response.ResponseBuilder builder = Response.status(status);
        if (masterResponse.rawBody() != null) {
            builder.entity(masterResponse.rawBody());
        }
        masterResponse.headers().forEach((key, values) -> {
            if (!"transfer-encoding".equalsIgnoreCase(key) && !"content-length".equalsIgnoreCase(key)) {
                values.forEach(value -> builder.header(key, value));
            }
        });
        return builder.build();
    }

    private URI buildUpstreamUri(String upstreamBase, UriInfo uriInfo) {
        String base = upstreamBase.endsWith("/") ? upstreamBase.substring(0, upstreamBase.length() - 1) : upstreamBase;
        String path = uriInfo.getRequestUri().getRawPath();
        String query = uriInfo.getRequestUri().getRawQuery();
        String url = base + path + (query != null ? "?" + query : "");
        return URI.create(url);
    }
}

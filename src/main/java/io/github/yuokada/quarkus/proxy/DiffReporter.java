package io.github.yuokada.quarkus.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yuokada.quarkus.proxy.model.DiffEntry;
import io.github.yuokada.quarkus.proxy.model.ComparisonResult;
import io.github.yuokada.quarkus.proxy.model.UpstreamResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DiffReporter {

    @Inject
    ObjectMapper objectMapper;

    public void report(
            String requestId,
            String clientRequestId,
            String method,
            String path,
            UpstreamResponse master,
            UpstreamResponse shadow,
            ComparisonResult result,
            List<DiffEntry> diffs) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", requestId);
        payload.put("clientRequestId", clientRequestId);
        payload.put("method", method);
        payload.put("path", path);
        payload.put("statusMaster", master.status());
        payload.put("statusShadow", shadow.status());
        payload.put("latencyMaster", master.latencyMs());
        payload.put("latencyShadow", shadow.latencyMs());
        payload.put("result", result.result().name());
        payload.put("diff", diffs);
        if (result.errorMessage() != null) {
            payload.put("error", result.errorMessage());
        }

        try {
            System.out.println(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            System.out.println("{\"result\":\"ERROR\",\"message\":\"failed to serialize report\"}");
        }
    }
}

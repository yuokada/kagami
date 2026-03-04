package io.github.yuokada.quarkus.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.github.yuokada.quarkus.proxy.model.ComparisonResult;
import io.github.yuokada.quarkus.proxy.model.DiffEntry;
import io.github.yuokada.quarkus.proxy.model.UpstreamResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
@SuppressWarnings("deprecation")
public class JsonComparator {

    private final ConcurrentMap<String, JsonPath> compiledPaths = new ConcurrentHashMap<>();

    @Inject
    ObjectMapper objectMapper;

    @Inject
    JsonNormalizer jsonNormalizer;

    public ComparisonResult compare(UpstreamResponse master, UpstreamResponse shadow, ProxyConfig.Compare compareConfig) {
        if (!compareConfig.enabled()) {
            return new ComparisonResult(ComparisonResult.Result.SAME, List.of(), null);
        }

        if (master.status() != shadow.status()) {
            return new ComparisonResult(ComparisonResult.Result.DIFF, diffStatus(master, shadow), null);
        }

        try {
            JsonNode masterNode = applyIgnorePaths(parseJson(master.decodedBody()), compareConfig);
            JsonNode shadowNode = applyIgnorePaths(parseJson(shadow.decodedBody()), compareConfig);
            String masterCanonical = jsonNormalizer.canonicalString(masterNode);
            String shadowCanonical = jsonNormalizer.canonicalString(shadowNode);
            if (masterCanonical.equals(shadowCanonical)) {
                return new ComparisonResult(ComparisonResult.Result.SAME, List.of(), null);
            }
            List<DiffEntry> diffs = new ArrayList<>();
            compareNodes(masterNode, shadowNode, "$", compareConfig, diffs);
            if (diffs.isEmpty()) {
                return new ComparisonResult(ComparisonResult.Result.SAME, List.of(), null);
            }
            return new ComparisonResult(ComparisonResult.Result.DIFF, diffs, null);
        } catch (IOException exception) {
            return new ComparisonResult(ComparisonResult.Result.ERROR, List.of(), exception.getMessage());
        }
    }

    private JsonNode parseJson(byte[] body) throws IOException {
        return objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
    }

    private JsonNode applyIgnorePaths(JsonNode node, ProxyConfig.Compare compareConfig) {
        if (compareConfig.ignorePaths().isEmpty()) {
            return node;
        }
        Object document = objectMapper.convertValue(node, Object.class);
        Configuration configuration = Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).build();
        DocumentContext context = JsonPath.using(configuration).parse(document);
        for (String path : compareConfig.ignorePaths()) {
            context.delete(compiledPath(path));
        }
        Object updated = context.json();
        if (updated == null) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.valueToTree(updated);
    }

    private JsonPath compiledPath(String path) {
        return compiledPaths.computeIfAbsent(path, JsonPath::compile);
    }

    private void compareNodes(
            JsonNode master,
            JsonNode shadow,
            String path,
            ProxyConfig.Compare compareConfig,
            List<DiffEntry> diffs) throws JsonProcessingException {
        if (master == null || master instanceof MissingNode) {
            handleMissing(master, shadow, path, compareConfig, diffs);
            return;
        }
        if (shadow == null || shadow instanceof MissingNode) {
            handleMissing(shadow, master, path, compareConfig, diffs);
            return;
        }

        if (master.isObject() && shadow.isObject()) {
            compareObjects(master, shadow, path, compareConfig, diffs);
            return;
        }

        if (master.isArray() && shadow.isArray()) {
            compareArrays(master, shadow, path, compareConfig, diffs);
            return;
        }

        if (!master.equals(shadow)) {
            diffs.add(new DiffEntry(path, master, shadow));
        }
    }

    private void compareObjects(
            JsonNode master,
            JsonNode shadow,
            String path,
            ProxyConfig.Compare compareConfig,
            List<DiffEntry> diffs) throws JsonProcessingException {
        TreeMap<String, JsonNode> masterFields = sortedFields(master);
        TreeMap<String, JsonNode> shadowFields = sortedFields(shadow);
        TreeMap<String, JsonNode> merged = new TreeMap<>(masterFields);
        merged.putAll(shadowFields);
        for (String key : merged.keySet()) {
            JsonNode masterValue = masterFields.getOrDefault(key, MissingNode.getInstance());
            JsonNode shadowValue = shadowFields.getOrDefault(key, MissingNode.getInstance());
            compareNodes(masterValue, shadowValue, path + "." + key, compareConfig, diffs);
        }
    }

    private void compareArrays(
            JsonNode master,
            JsonNode shadow,
            String path,
            ProxyConfig.Compare compareConfig,
            List<DiffEntry> diffs) throws JsonProcessingException {
        if (!compareConfig.arrayOrderSensitive()) {
            List<String> masterCanonical = toCanonicalElements(master);
            List<String> shadowCanonical = toCanonicalElements(shadow);
            masterCanonical.sort(String::compareTo);
            shadowCanonical.sort(String::compareTo);
            if (!masterCanonical.equals(shadowCanonical)) {
                diffs.add(new DiffEntry(path, master, shadow));
            }
            return;
        }

        int max = Math.max(master.size(), shadow.size());
        for (int i = 0; i < max; i++) {
            JsonNode masterValue = i < master.size() ? master.get(i) : MissingNode.getInstance();
            JsonNode shadowValue = i < shadow.size() ? shadow.get(i) : MissingNode.getInstance();
            compareNodes(masterValue, shadowValue, path + "[" + i + "]", compareConfig, diffs);
        }
    }

    private void handleMissing(
            JsonNode missingNode,
            JsonNode presentNode,
            String path,
            ProxyConfig.Compare compareConfig,
            List<DiffEntry> diffs) {
        if (compareConfig.nullVsMissingEqual() && presentNode != null && presentNode.isNull()) {
            return;
        }
        diffs.add(new DiffEntry(path, missingNode, presentNode));
    }

    private TreeMap<String, JsonNode> sortedFields(JsonNode node) {
        TreeMap<String, JsonNode> fields = new TreeMap<>();
        node.fields().forEachRemaining(entry -> fields.put(entry.getKey(), entry.getValue()));
        return fields;
    }

    private List<String> toCanonicalElements(JsonNode node) throws JsonProcessingException {
        List<String> canonical = new ArrayList<>();
        for (JsonNode element : node) {
            canonical.add(jsonNormalizer.canonicalString(element));
        }
        return canonical;
    }

    private List<DiffEntry> diffStatus(UpstreamResponse master, UpstreamResponse shadow) {
        return List.of(new DiffEntry("$.status", master.status(), shadow.status()));
    }
}

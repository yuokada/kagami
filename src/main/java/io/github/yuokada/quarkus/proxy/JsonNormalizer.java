package io.github.yuokada.quarkus.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.TreeMap;

@ApplicationScoped
@SuppressWarnings("deprecation")
public class JsonNormalizer {

    @Inject
    ObjectMapper objectMapper;

    public JsonNode normalize(JsonNode node) {
        return canonicalize(node);
    }

    public String canonicalString(JsonNode node) throws com.fasterxml.jackson.core.JsonProcessingException {
        return objectMapper.writeValueAsString(normalize(node));
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = JsonNodeFactory.instance.objectNode();
            TreeMap<String, JsonNode> fields = new TreeMap<>();
            node.fields().forEachRemaining(entry -> fields.put(entry.getKey(), entry.getValue()));
            fields.forEach((key, value) -> sorted.set(key, canonicalize(value)));
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            node.forEach(element -> array.add(canonicalize(element)));
            return array;
        }
        return node;
    }
}

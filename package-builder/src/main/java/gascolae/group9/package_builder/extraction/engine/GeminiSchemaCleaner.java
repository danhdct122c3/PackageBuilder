package gascolae.group9.package_builder.extraction.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Set;

/**
 * Port make_gemini_schema.clean(): bỏ keyword Gemini không hỗ trợ ($schema, maxLength, minLength, uniqueItems)
 * và đổi enum có null sang anyOf. Dùng để dựng schema rút gọn cho chế độ form.
 */
public final class GeminiSchemaCleaner {
    private static final Set<String> DROP = Set.of("$schema", "maxLength", "minLength", "uniqueItems");

    private GeminiSchemaCleaner() {
    }

    public static JsonNode clean(JsonNode node) {
        JsonNodeFactory f = JsonNodeFactory.instance;
        if (node.isObject()) {
            ObjectNode out = f.objectNode();
            node.fields().forEachRemaining(e -> {
                if (!DROP.contains(e.getKey())) {
                    out.set(e.getKey(), clean(e.getValue()));
                }
            });
            JsonNode en = out.get("enum");
            if (en != null && en.isArray() && containsNull(en)) {
                ArrayNode values = f.arrayNode();
                en.forEach(v -> {
                    if (!v.isNull()) {
                        values.add(v);
                    }
                });
                ObjectNode result = f.objectNode();
                out.fields().forEachRemaining(e -> {
                    if (!"enum".equals(e.getKey()) && !"type".equals(e.getKey())) {
                        result.set(e.getKey(), e.getValue());
                    }
                });
                ArrayNode anyOf = result.putArray("anyOf");
                anyOf.addObject().put("type", "string").set("enum", values);
                anyOf.addObject().put("type", "null");
                return result;
            }
            return out;
        }
        if (node.isArray()) {
            ArrayNode out = f.arrayNode();
            node.forEach(x -> out.add(clean(x)));
            return out;
        }
        return node;
    }

    private static boolean containsNull(JsonNode arr) {
        for (JsonNode v : arr) {
            if (v.isNull()) {
                return true;
            }
        }
        return false;
    }
}

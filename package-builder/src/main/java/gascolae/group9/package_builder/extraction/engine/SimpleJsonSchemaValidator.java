package gascolae.group9.package_builder.extraction.engine;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bộ kiểm JSON Schema rút gọn, đủ cho extraction_schema.json (type, enum, required, properties,
 * additionalProperties=false, items, maxLength, minimum, maximum, uniqueItems).
 * Trả lỗi đầu tiên tìm thấy, giống cách gemini_extract.py chỉ ghi dòng đầu của ValidationError.
 */
public final class SimpleJsonSchemaValidator {
    private SimpleJsonSchemaValidator() {
    }

    public static Optional<String> firstError(JsonNode data, JsonNode schema) {
        return check(data, schema, "$");
    }

    private static Optional<String> check(JsonNode node, JsonNode schema, String path) {
        if (schema == null || schema.isMissingNode()) {
            return Optional.empty();
        }
        JsonNode type = schema.get("type");
        if (type != null && !matchesType(node, type)) {
            return Optional.of(path + ": " + render(node) + " is not of type " + type);
        }
        JsonNode en = schema.get("enum");
        if (en != null && en.isArray()) {
            boolean ok = false;
            for (JsonNode e : en) {
                if (e.equals(node) || (e.isNull() && node.isNull())) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                return Optional.of(path + ": " + render(node) + " is not one of " + en);
            }
        }
        if (node.isTextual() && schema.has("maxLength")
                && node.asText().codePointCount(0, node.asText().length()) > schema.get("maxLength").asInt()) {
            return Optional.of(path + ": " + render(node) + " is too long");
        }
        if (node.isNumber()) {
            if (schema.has("minimum") && node.asDouble() < schema.get("minimum").asDouble()) {
                return Optional.of(path + ": " + node + " is less than the minimum of " + schema.get("minimum"));
            }
            if (schema.has("maximum") && node.asDouble() > schema.get("maximum").asDouble()) {
                return Optional.of(path + ": " + node + " is greater than the maximum of " + schema.get("maximum"));
            }
        }
        if (node.isArray()) {
            if (schema.path("uniqueItems").asBoolean(false)) {
                Set<JsonNode> seen = new HashSet<>();
                for (JsonNode item : node) {
                    if (!seen.add(item)) {
                        return Optional.of(path + ": " + node + " has non-unique elements");
                    }
                }
            }
            JsonNode items = schema.get("items");
            int i = 0;
            for (JsonNode item : node) {
                Optional<String> err = check(item, items, path + "[" + i++ + "]");
                if (err.isPresent()) {
                    return err;
                }
            }
        }
        if (node.isObject()) {
            JsonNode required = schema.get("required");
            if (required != null) {
                for (JsonNode r : required) {
                    if (!node.has(r.asText())) {
                        return Optional.of(path + ": '" + r.asText() + "' is a required property");
                    }
                }
            }
            JsonNode props = schema.path("properties");
            boolean closed = schema.has("additionalProperties") && !schema.get("additionalProperties").asBoolean(true);
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> f = it.next();
                if (props.has(f.getKey())) {
                    Optional<String> err = check(f.getValue(), props.get(f.getKey()), path + "." + f.getKey());
                    if (err.isPresent()) {
                        return err;
                    }
                } else if (closed) {
                    return Optional.of(path + ": Additional properties are not allowed ('" + f.getKey() + "' was unexpected)");
                }
            }
        }
        return Optional.empty();
    }

    private static boolean matchesType(JsonNode node, JsonNode type) {
        if (type.isArray()) {
            for (JsonNode t : type) {
                if (matchesType(node, t)) {
                    return true;
                }
            }
            return false;
        }
        return switch (type.asText()) {
            case "string" -> node.isTextual();
            case "number" -> node.isNumber();
            case "integer" -> node.isIntegralNumber() || (node.isNumber() && node.asDouble() == Math.rint(node.asDouble()));
            case "boolean" -> node.isBoolean();
            case "array" -> node.isArray();
            case "object" -> node.isObject();
            case "null" -> node.isNull();
            default -> true;
        };
    }

    private static String render(JsonNode node) {
        String s = node.toString();
        return s.length() > 80 ? s.substring(0, 80) + "…" : s;
    }
}

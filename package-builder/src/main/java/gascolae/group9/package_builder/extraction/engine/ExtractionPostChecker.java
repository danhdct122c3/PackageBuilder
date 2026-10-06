package gascolae.group9.package_builder.extraction.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hậu kiểm JSON Gemini trả về, đúng thứ tự trong BAN_GIAO_SV1 mục 6 và hàm postcheck() của gemini_extract.py:
 * 1) gộp mã trùng (giữ thứ tự) → 2) loại mã không có trong danh sách → 3) kiểm schema (sai thì cảnh báo, vẫn dùng phần hợp lệ).
 */
public class ExtractionPostChecker {
    private final JsonNode schema;
    private final Map<String, Set<String>> allowed;

    public ExtractionPostChecker(JsonNode schema) {
        this.schema = schema;
        this.allowed = allowedCodes(schema);
    }

    /** Danh sách mã hợp lệ của từng trường mảng, lấy từ enum trong schema (giống allowed_codes()). */
    public static Map<String, Set<String>> allowedCodes(JsonNode schema) {
        Map<String, Set<String>> out = new LinkedHashMap<>();
        schema.path("properties").fields().forEachRemaining(e -> {
            JsonNode spec = e.getValue();
            JsonNode en = spec.path("items").path("enum");
            if ("array".equals(spec.path("type").asText()) && en.isArray() && !en.isEmpty()) {
                Set<String> codes = new LinkedHashSet<>();
                en.forEach(c -> codes.add(c.asText()));
                out.put(e.getKey(), codes);
            }
        });
        return out;
    }

    public Map<String, Set<String>> getAllowed() {
        return allowed;
    }

    /** Sửa {@code data} tại chỗ và trả danh sách cảnh báo. */
    public List<String> postcheck(ObjectNode data) {
        List<String> warnings = new ArrayList<>();
        allowed.forEach((field, ok) -> {
            JsonNode node = data.get(field);
            List<String> vals = new ArrayList<>();
            if (node != null && node.isArray()) {
                node.forEach(v -> vals.add(v.isTextual() ? v.asText() : v.toString()));
            }
            List<String> uniq = new ArrayList<>(new LinkedHashSet<>(vals));
            List<String> current = vals;
            if (uniq.size() < vals.size()) {
                warnings.add("Gộp mã trùng ở " + field + ": " + PyText.reprList(vals) + " → " + PyText.reprList(uniq));
                data.set(field, toArray(data, uniq));
                current = uniq;
            }
            List<String> bad = current.stream().filter(v -> !ok.contains(v)).toList();
            if (!bad.isEmpty()) {
                warnings.add("Loại mã không hợp lệ ở " + field + ": " + PyText.reprList(bad));
                data.set(field, toArray(data, current.stream().filter(ok::contains).toList()));
            }
        });
        SimpleJsonSchemaValidator.firstError(data, schema).ifPresent(err -> warnings.add("Sai schema: " + err));
        return warnings;
    }

    /** Đổi JSON đã hậu kiểm thành phiếu trích xuất. */
    public static ExtractionResult toResult(ObjectNode data) {
        ExtractionResult r = new ExtractionResult();
        r.setProjectName(text(data, "project_name"));
        r.setObjectiveRaw(text(data, "objective_raw"));
        r.setIndustryRaw(text(data, "industry_raw"));
        r.setEnvironmentRaw(text(data, "environment_raw"));
        JsonNode av = data.get("area_value");
        if (av != null && av.isNumber()) {
            r.setAreaValue(av.asDouble());
        }
        r.setAreaUnit(text(data, "area_unit"));
        for (String field : ExtractionResult.CODE_FIELDS) {
            List<String> codes = new ArrayList<>();
            JsonNode arr = data.get(field);
            if (arr != null && arr.isArray()) {
                arr.forEach(c -> {
                    if (c.isTextual()) {
                        codes.add(c.asText());
                    }
                });
            }
            r.setCodes(field, codes);
        }
        JsonNode conf = data.get("confidence");
        if (conf != null && conf.isObject()) {
            conf.fields().forEachRemaining(e -> {
                if (e.getValue().isNumber()) {
                    r.getConfidence().put(e.getKey(), e.getValue().asDouble());
                }
            });
        }
        r.setNotes(text(data, "notes"));
        return r;
    }

    private static String text(JsonNode data, String field) {
        JsonNode n = data.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }

    private static ArrayNode toArray(ObjectNode data, List<String> values) {
        ArrayNode arr = data.arrayNode();
        values.forEach(arr::add);
        return arr;
    }
}

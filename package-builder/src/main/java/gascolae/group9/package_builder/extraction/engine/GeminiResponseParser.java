package gascolae.group9.package_builder.extraction.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Đọc response REST của Gemini Interactions API (port output_text() và parse_json() trong gemini_extract.py).
 * Response REST không có trường output_text như SDK, nên dò mọi trường "text", bỏ qua usage, phần input và khối thought.
 */
public final class GeminiResponseParser {
    private static final Set<String> SKIP = Set.of(
            "usage", "input", "system_instruction", "response_format", "generation_config");
    private static final Pattern FIRST_OBJECT = Pattern.compile("\\{.*\\}", Pattern.DOTALL);

    private GeminiResponseParser() {
    }

    public static String outputText(JsonNode resp) {
        JsonNode direct = resp.get("output_text");
        if (direct != null && direct.isTextual()) {
            return direct.asText();
        }
        List<String> found = new ArrayList<>();
        walk(resp, found);
        if (found.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy phần chữ trong response Gemini");
        }
        return String.join("", found);
    }

    private static void walk(JsonNode node, List<String> found) {
        if (node.isObject()) {
            String type = node.path("type").asText("");
            if ("thought".equals(type) || "thought_summary".equals(type) || node.path("thought").asBoolean(false)) {
                return;
            }
            var it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (SKIP.contains(e.getKey())) {
                    continue;
                }
                if ("text".equals(e.getKey()) && e.getValue().isTextual()) {
                    found.add(e.getValue().asText());
                } else {
                    walk(e.getValue(), found);
                }
            }
        } else if (node.isArray()) {
            node.forEach(child -> walk(child, found));
        }
    }

    /** Lấy đoạn {...} đầu tiên (tham lam, giống re.search(r"\{.*\}", text, re.S)) rồi parse. */
    public static ObjectNode parseJson(String text, ObjectMapper mapper) throws JsonProcessingException {
        String t = text.strip();
        Matcher m = FIRST_OBJECT.matcher(t);
        JsonNode node = mapper.readTree(m.find() ? m.group() : t);
        if (!(node instanceof ObjectNode obj)) {
            throw new IllegalStateException("Gemini không trả về một object JSON");
        }
        return obj;
    }
}

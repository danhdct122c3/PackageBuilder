package gascolae.group9.package_builder.extraction.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/** Đọc file đáp án sinh từ bản Python của SV3 (src/test/resources/sv3_reference, xem gen_reference.py). */
public final class Sv3Fixtures {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    private Sv3Fixtures() {
    }

    public static JsonNode load(String path) throws Exception {
        try (InputStream is = Sv3Fixtures.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Thiếu file test " + path);
            }
            return MAPPER.readTree(is);
        }
    }

    public static Map<String, Map<String, List<String>>> keywords() throws Exception {
        try (InputStream is = Sv3Fixtures.class.getResourceAsStream("/ai/keywords.json")) {
            return MAPPER.readValue(is, new TypeReference<>() {
            });
        }
    }

    public static List<String> strings(JsonNode arr) {
        return arr == null || arr.isNull() ? List.of() : MAPPER.convertValue(arr, new TypeReference<List<String>>() {
        });
    }
}

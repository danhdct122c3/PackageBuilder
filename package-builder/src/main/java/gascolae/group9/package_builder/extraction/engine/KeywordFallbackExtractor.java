package gascolae.group9.package_builder.extraction.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lớp dự phòng của D6: trích xuất bằng bảng từ khóa (port từ fallback_extract.py + keywords.py).
 * Chạy khi cả hai model Gemini đều lỗi, không cần mạng. Kết quả luôn ở mức "cần Sales xác nhận".
 */
public class KeywordFallbackExtractor {
    public static final double FALLBACK_OVERALL = 0.3;
    public static final String FALLBACK_NOTES =
            "Trích bằng bảng từ khóa dự phòng, Sales phải xác nhận trước khi chạy gợi ý.";

    private static final Map<String, String> GROUP_TO_FIELD = Map.of(
            "OBJECTIVE", ExtractionResult.OBJECTIVE,
            "ENVIRONMENT", ExtractionResult.ENVIRONMENT,
            "INDUSTRY", ExtractionResult.INDUSTRY,
            "TOPIC", ExtractionResult.TOPIC,
            "EXPECTED_OUTPUT", ExtractionResult.EXPECTED_OUTPUT,
            "AVAILABLE_INPUT", ExtractionResult.AVAILABLE_INPUT);

    private static final Map<String, String> UNITS = Map.of(
            "ha", "ha", "héc ta", "ha", "hecta", "ha",
            "km2", "km2", "km²", "km2", "km", "km", "m2", "m2", "m²", "m2");

    // Python: r'(\d[\d,]*)\s*(ha|héc ta|hecta|km2|km²|km|m2|m²)\b' — \b của Python coi '²' là ký tự chữ/số,
    // nên thay \b bằng "ký tự sau không phải chữ/số/_" để khớp hành vi.
    private static final Pattern AREA = Pattern.compile(
            "(\\d[\\d,]*)\\s*(ha|héc ta|hecta|km2|km²|km|m2|m²)(?![\\p{L}\\p{N}_])",
            Pattern.UNICODE_CHARACTER_CLASS);

    /** nhóm → (mã → từ khóa đã bỏ dấu), giữ đúng thứ tự trong keywords.json. */
    private final Map<String, Map<String, List<String>>> normalized = new LinkedHashMap<>();

    public KeywordFallbackExtractor(Map<String, Map<String, List<String>>> keywordTable) {
        keywordTable.forEach((group, table) -> {
            Map<String, List<String>> codes = new LinkedHashMap<>();
            table.forEach((code, kws) -> codes.put(code, kws.stream().map(PyText::stripAccents).toList()));
            normalized.put(group, codes);
        });
    }

    public ExtractionResult extract(String text) {
        String n = PyText.stripAccents(text);
        ExtractionResult out = new ExtractionResult();
        normalized.forEach((group, table) -> {
            List<String> hits = new ArrayList<>();
            table.forEach((code, kws) -> {
                if (kws.stream().anyMatch(n::contains)) {
                    hits.add(code);
                }
            });
            String field = GROUP_TO_FIELD.get(group);
            if (field != null) {
                out.setCodes(field, hits);
            }
        });
        double[] area = new double[1];
        String unit = parseArea(text, area);
        if (unit != null) {
            out.setAreaValue(area[0]);
            out.setAreaUnit(unit);
        }
        out.setLayer(ExtractionResult.LAYER_FALLBACK);
        out.getConfidence().put("overall", FALLBACK_OVERALL);
        out.setNotes(FALLBACK_NOTES);
        return out;
    }

    /** Trả đơn vị (hoặc null) và ghi giá trị vào value[0]. */
    static String parseArea(String text, double[] value) {
        if (text == null) {
            return null;
        }
        String t = text.toLowerCase(Locale.ROOT).replace('.', ',');
        Matcher m = AREA.matcher(t);
        if (!m.find()) {
            return null;
        }
        value[0] = Double.parseDouble(m.group(1).replace(",", ""));
        return UNITS.get(m.group(2));
    }
}

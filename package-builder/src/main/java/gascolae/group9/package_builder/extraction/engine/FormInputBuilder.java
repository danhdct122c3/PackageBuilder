package gascolae.group9.package_builder.extraction.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dựng đoạn input cho AI từ dữ liệu form (port form_extract.py).
 * Thông tin cá nhân (tên người liên hệ, email, số điện thoại) không bao giờ được gửi sang Gemini.
 */
public final class FormInputBuilder {
    public static final String FORM_INSTRUCTION = """
            Dữ liệu dưới đây do Sales nhập qua form. Mỗi dòng là một trường đã có nhãn.
            Nhiệm vụ: chỉ GÁN MÃ theo danh sách hợp lệ. Không chép lại *_raw, không sửa dữ liệu khách.
            - "Dữ liệu khách đã có" chỉ dùng cho available_input_codes.
            - "Đầu ra mong muốn" dùng cho expected_output_codes. Nếu trường này không có, chỉ chọn đầu ra suy ra rõ ràng từ mục tiêu.
            - "Tên đơn vị" chỉ dùng để suy ra industry_codes.
            - Trường nào không có trong danh sách dưới đây nghĩa là khách để trống.

            """;
    public static final String JSON_ONLY = "\n\nTrả về đúng một object JSON, không thêm chữ nào ngoài JSON.";

    /** (tên trường form, nhãn đưa cho AI) — đúng thứ tự AI_FIELDS trong form_extract.py. */
    private static final List<String[]> AI_FIELDS = List.of(
            new String[]{"objectiveRaw", "Mục tiêu"},
            new String[]{"expectedOutputs", "Đầu ra mong muốn"},
            new String[]{"providedInputsRaw", "Dữ liệu khách đã có"},
            new String[]{"locationDescription", "Địa điểm / hiện trường"},
            new String[]{"companyName", "Tên đơn vị (chỉ dùng để suy ra ngành)"},
            new String[]{"projectName", "Tên dự án"});
    private static final Set<String> NEVER_SEND = Set.of("customerName", "contactEmail", "contactPhone");
    private static final List<String> FORM_LABEL_FIELDS = ExtractionResult.CODE_FIELDS;

    private static final Map<String, String> UNITS = Map.of(
            "ha", "ha", "héc ta", "ha", "hecta", "ha", "hec ta", "ha",
            "km2", "km2", "km²", "km2", "km", "km", "m2", "m2", "m²", "m2");
    private static final Pattern AREA = Pattern.compile(
            "(\\d+(?:[.,]\\d+)*)\\s*(héc ta|hec ta|hecta|ha|km2|km²|km|m2|m²)?", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern THOUSANDS = Pattern.compile("\\d{1,3}([.,]\\d{3})+", Pattern.UNICODE_CHARACTER_CLASS);

    public record FormInput(String aiText, String keywordText, List<String> fieldsSentToAi, String userInput) {
        public boolean isEmpty() {
            return aiText.isEmpty();
        }
    }

    public record Area(Double value, String unit, String warning) {
    }

    private FormInputBuilder() {
    }

    public static FormInput build(Map<String, ?> form) {
        List<String> lines = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<String> sent = new ArrayList<>();
        for (String[] f : AI_FIELDS) {
            if (NEVER_SEND.contains(f[0])) {
                continue;
            }
            Object raw = form.get(f[0]);
            String v = raw instanceof String s ? s.strip() : (raw == null ? null : raw.toString());
            if (v != null && !v.isEmpty()) {
                lines.add("- " + f[1] + ": " + v);
                values.add(v);
                sent.add(f[0]);
            }
        }
        String aiText = String.join("\n", lines);
        return new FormInput(aiText, String.join("\n", values), sent, FORM_INSTRUCTION + aiText + JSON_ONLY);
    }

    /** '200 ha' → (200, ha); '2.000 ha' / '2,000 ha' → (2000, ha); '1,5 km2' → (1.5, km2). */
    public static Area parseArea(Object raw) {
        if (raw == null || raw.toString().strip().isEmpty()) {
            return new Area(null, null, null);
        }
        if (raw instanceof Number n) {
            return new Area(n.doubleValue(), null, "Diện tích không có đơn vị");
        }
        String t = raw.toString().strip().toLowerCase(Locale.ROOT);
        Matcher m = AREA.matcher(t);
        if (!m.find()) {
            return new Area(null, null, "Không đọc được diện tích: " + pyRepr(raw.toString()));
        }
        String num = m.group(1);
        double value = THOUSANDS.matcher(num).matches()
                ? Double.parseDouble(num.replaceAll("[.,]", ""))
                : Double.parseDouble(num.replace(",", "."));
        String unit = m.group(2) == null ? null : UNITS.get(m.group(2));
        return new Area(value, unit, unit == null ? "Diện tích thiếu đơn vị: " + pyRepr(raw.toString()) : null);
    }

    /** Schema rút gọn cho chế độ form: chỉ phần gán mã + confidence + notes (giống form_schemas()). */
    public static ObjectNode formSchema(JsonNode fullSchema) {
        ObjectNode sub = fullSchema.deepCopy();
        sub.put("title", fullSchema.path("title").asText() + " — form mode");
        ObjectNode props = sub.putObject("properties");
        for (String k : FORM_LABEL_FIELDS) {
            props.set(k, fullSchema.path("properties").path(k).deepCopy());
        }
        props.set("confidence", fullSchema.path("properties").path("confidence").deepCopy());
        props.set("notes", fullSchema.path("properties").path("notes").deepCopy());
        sub.putArray("required").add(ExtractionResult.OBJECTIVE).add("confidence");
        return sub;
    }

    private static String pyRepr(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}

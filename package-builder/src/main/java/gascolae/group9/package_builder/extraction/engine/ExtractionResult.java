package gascolae.group9.package_builder.extraction.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phiếu trích xuất sau hậu kiểm (D6). Tên mã theo schema {@code Requirement Extraction v1.1}.
 * Không phụ thuộc Spring để kiểm thử đối chiếu với bản Python.
 */
public class ExtractionResult {
    public static final String OBJECTIVE = "objective_codes";
    public static final String ENVIRONMENT = "environment_codes";
    public static final String INDUSTRY = "industry_codes";
    public static final String TOPIC = "topic_codes";
    public static final String EXPECTED_OUTPUT = "expected_output_codes";
    public static final String AVAILABLE_INPUT = "available_input_codes";
    public static final List<String> CODE_FIELDS =
            List.of(OBJECTIVE, ENVIRONMENT, INDUSTRY, TOPIC, EXPECTED_OUTPUT, AVAILABLE_INPUT);

    public static final String LAYER_FALLBACK = "FALLBACK_KEYWORD";

    private String projectName;
    private String objectiveRaw;
    private String industryRaw;
    private String environmentRaw;
    private Double areaValue;
    private String areaUnit;
    private final Map<String, List<String>> codes = new LinkedHashMap<>();
    private final Map<String, Double> confidence = new LinkedHashMap<>();
    private String notes;
    private String layer;
    private Long latencyMs;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> previousErrors = new ArrayList<>();

    public ExtractionResult() {
        CODE_FIELDS.forEach(f -> codes.put(f, new ArrayList<>()));
    }

    public List<String> codes(String field) {
        return codes.computeIfAbsent(field, k -> new ArrayList<>());
    }

    public void setCodes(String field, List<String> values) {
        codes.put(field, new ArrayList<>(values == null ? List.of() : values));
    }

    public Map<String, List<String>> getCodes() {
        return codes;
    }

    public double overall() {
        Double v = confidence.get("overall");
        return v == null ? 0d : v;
    }

    public boolean isFallback() {
        return LAYER_FALLBACK.equals(layer);
    }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getObjectiveRaw() { return objectiveRaw; }
    public void setObjectiveRaw(String objectiveRaw) { this.objectiveRaw = objectiveRaw; }
    public String getIndustryRaw() { return industryRaw; }
    public void setIndustryRaw(String industryRaw) { this.industryRaw = industryRaw; }
    public String getEnvironmentRaw() { return environmentRaw; }
    public void setEnvironmentRaw(String environmentRaw) { this.environmentRaw = environmentRaw; }
    public Double getAreaValue() { return areaValue; }
    public void setAreaValue(Double areaValue) { this.areaValue = areaValue; }
    public String getAreaUnit() { return areaUnit; }
    public void setAreaUnit(String areaUnit) { this.areaUnit = areaUnit; }
    public Map<String, Double> getConfidence() { return confidence; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getLayer() { return layer; }
    public void setLayer(String layer) { this.layer = layer; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getPreviousErrors() { return previousErrors; }
}

package gascolae.group9.package_builder.recommendation.engine;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cấu hình chấm điểm D4, đọc từ matching_rules.json (đổi số trong file đó thì không phải sửa code).
 */
public record MatchingRules(
        Map<String, Integer> weights,
        int exact,
        int viaParentTag,
        int sameOutputFamily,
        int none,
        double minMatchScore,
        int maxResults,
        boolean activeOnly,
        List<String> lifecycleExcluded,
        String version) {

    public static final List<String> SIGNALS = List.of("objective", "environment", "industry", "topic", "expected_output");

    public static MatchingRules from(JsonNode cfg) {
        Map<String, Integer> w = new LinkedHashMap<>();
        cfg.path("weights").fields().forEachRemaining(e -> w.put(e.getKey(), e.getValue().asInt()));
        JsonNode p = cfg.path("match_points");
        List<String> excluded = new ArrayList<>();
        cfg.path("guardrails").path("lifecycle_status_excluded").forEach(x -> excluded.add(x.asText()));
        return new MatchingRules(w,
                p.path("exact").asInt(100),
                p.path("via_parent_tag").asInt(60),
                p.path("same_output_family").asInt(70),
                p.path("none").asInt(0),
                cfg.path("display").path("min_match_score").asDouble(40),
                cfg.path("display").path("max_results").asInt(8),
                cfg.path("guardrails").path("active_only").asBoolean(true),
                excluded,
                cfg.path("version").asText(""));
    }

    public int weight(String signal) {
        return weights.getOrDefault(signal, 0);
    }
}

package gascolae.group9.package_builder.recommendation.engine;

import gascolae.group9.package_builder.extraction.engine.PyText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Engine chấm điểm D4 — port 1:1 score.py v1.1 của SV3.
 * <ul>
 *   <li>Mỗi tín hiệu 0–100 theo độ bao phủ: khớp đủ 100, khớp qua mã cha 60, đầu ra cùng nhóm 70, không khớp 0.</li>
 *   <li>match_score = Σ(trọng số × điểm) / Σ trọng số của các tín hiệu có dữ liệu (tín hiệu khách không nêu bị bỏ, không chấm 0).</li>
 *   <li>Chỉ xét dịch vụ active, không ARCHIVED; shortlist match ≥ ngưỡng, tối đa N, xếp match ↓, objective ↓, service_code ↑.</li>
 * </ul>
 */
public class RecommendationScorer {
    private static final Map<String, String> LABELS = Map.of(
            "objective", "Mục tiêu", "environment", "Hiện trường", "industry", "Ngành",
            "expected_output", "Đầu ra", "topic", "Chủ đề");
    private static final List<String> REASON_ORDER = List.of("objective", "expected_output", "environment", "industry", "topic");
    private static final Map<String, String> SIGNAL_TAG_TYPE = Map.of(
            "objective", "OBJECTIVE", "environment", "ENVIRONMENT", "industry", "INDUSTRY", "topic", "TOPIC");

    private final MatchingRules rules;
    private final CatalogSnapshot catalog;

    public RecommendationScorer(MatchingRules rules, CatalogSnapshot catalog) {
        this.rules = rules;
        this.catalog = catalog;
    }

    /** Phiếu đầu vào: 5 mảng mã theo tín hiệu (objective, environment, industry, topic, expected_output). */
    public record Requirement(Map<String, List<String>> codes) {
        public List<String> get(String signal) {
            return codes.getOrDefault(signal, List.of());
        }

        public static Requirement of(List<String> objective, List<String> environment, List<String> industry,
                                     List<String> topic, List<String> expectedOutput) {
            Map<String, List<String>> m = new LinkedHashMap<>();
            m.put("objective", nz(objective));
            m.put("environment", nz(environment));
            m.put("industry", nz(industry));
            m.put("topic", nz(topic));
            m.put("expected_output", nz(expectedOutput));
            return new Requirement(m);
        }

        private static List<String> nz(List<String> l) {
            return l == null ? List.of() : l;
        }
    }

    public static final class ScoreRow {
        private final CatalogSnapshot.ServiceInfo service;
        private final double matchScore;
        private final boolean anySignal;
        private final Map<String, Double> signals;
        private final Map<String, List<String>> detail;
        private String reason;

        ScoreRow(CatalogSnapshot.ServiceInfo service, double matchScore, boolean anySignal,
                 Map<String, Double> signals, Map<String, List<String>> detail) {
            this.service = service;
            this.matchScore = matchScore;
            this.anySignal = anySignal;
            this.signals = signals;
            this.detail = detail;
        }

        public CatalogSnapshot.ServiceInfo getService() { return service; }
        public String getServiceCode() { return service.code(); }
        public double getMatchScore() { return matchScore; }
        /** null = khách không nêu tín hiệu này (Python None); khi lưu DB thì ghi 0. */
        public Double getObjectiveScore() { return signals.get("objective"); }
        public Double getUseCaseScore() { return signals.get("environment"); }
        public Double getIndustryScore() { return signals.get("industry"); }
        public Double getOutputScore() { return signals.get("expected_output"); }
        public Double getTagScore() { return signals.get("topic"); }
        public Map<String, List<String>> getDetail() { return detail; }
        public String getReason() { return reason; }

        String matchScoreText() {
            return anySignal ? PyText.pyFloat(matchScore) : "0";
        }
    }

    public record Result(List<ScoreRow> all, List<ScoreRow> shortlist) {
    }

    public Result score(Requirement req) {
        List<ScoreRow> rows = new ArrayList<>();
        for (CatalogSnapshot.ServiceInfo s : catalog.getServices()) {
            if (rules.activeOnly() && !s.active()) {
                continue;
            }
            if (rules.lifecycleExcluded().contains(s.lifecycleStatus())) {
                continue;
            }
            Map<String, Double> sig = new LinkedHashMap<>();
            Map<String, List<String>> det = new LinkedHashMap<>();
            for (String signal : MatchingRules.SIGNALS) {
                List<String> d = new ArrayList<>();
                Double v = "expected_output".equals(signal)
                        ? outputSignal(req.get(signal), s.code(), d)
                        : tagSignal(req.get(signal), catalog.tagsOf(s.code(), SIGNAL_TAG_TYPE.get(signal)), d);
                sig.put(signal, v);
                det.put(signal, d);
            }
            // Cộng theo đúng thứ tự objective, environment, industry, topic, expected_output như dict của Python.
            double num = 0;
            int tot = 0;
            for (Map.Entry<String, Double> e : sig.entrySet()) {
                if (e.getValue() != null) {
                    num += rules.weight(e.getKey()) * e.getValue();
                    tot += rules.weight(e.getKey());
                }
            }
            double match = tot != 0 ? PyText.round2(num / tot) : 0;
            Map<String, List<String>> nonEmpty = det.entrySet().stream()
                    .filter(e -> !e.getValue().isEmpty())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
            rows.add(new ScoreRow(s, match, tot != 0, sig, nonEmpty));
        }
        rows.sort(Comparator
                .comparingDouble((ScoreRow r) -> -r.getMatchScore())
                .thenComparingDouble(r -> -(r.getObjectiveScore() == null ? 0 : r.getObjectiveScore()))
                .thenComparing(ScoreRow::getServiceCode));
        rows.forEach(r -> r.reason = reason(r));
        List<ScoreRow> keep = rows.stream()
                .filter(r -> r.getMatchScore() >= rules.minMatchScore())
                .limit(rules.maxResults())
                .toList();
        return new Result(rows, keep);
    }

    private Double tagSignal(List<String> reqCodes, Set<String> svcCodes, List<String> detail) {
        if (reqCodes.isEmpty()) {
            return null;
        }
        long sum = 0;
        for (String c : reqCodes) {
            String parent = catalog.parentOf(c);
            if (svcCodes.contains(c)) {
                sum += rules.exact();
                detail.add(c + " khớp đủ");
            } else if (parent != null && svcCodes.contains(parent)) {
                sum += rules.viaParentTag();
                detail.add(c + " khớp qua mã cha " + parent);
            } else {
                sum += rules.none();
            }
        }
        return PyText.round2((double) sum / reqCodes.size());
    }

    private Double outputSignal(List<String> reqOutputs, String serviceCode, List<String> detail) {
        if (reqOutputs.isEmpty()) {
            return null;
        }
        Set<String> outs = catalog.outputsOf(serviceCode);
        Set<String> fams = outs.stream().map(catalog::familyOf).filter(f -> !f.isEmpty()).collect(Collectors.toSet());
        long sum = 0;
        for (String c : reqOutputs) {
            String fam = catalog.familyOf(c);
            if (outs.contains(c)) {
                sum += rules.exact();
                detail.add(c + " khớp đủ");
            } else if (!fam.isEmpty() && fams.contains(fam)) {
                sum += rules.sameOutputFamily();
                detail.add(c + " khớp cùng nhóm " + fam);
            } else {
                sum += rules.none();
            }
        }
        return PyText.round2((double) sum / reqOutputs.size());
    }

    /** Mẫu câu recommendation_reason (hàm reason() trong score.py). */
    String reason(ScoreRow row) {
        List<String> parts = new ArrayList<>();
        for (String k : REASON_ORDER) {
            List<String> d = row.getDetail().get(k);
            if (d != null && !d.isEmpty()) {
                parts.add(LABELS.get(k) + " — " + d.stream().map(catalog::nice).collect(Collectors.joining("; ")));
            }
        }
        String s = parts.isEmpty() ? "Không khớp tín hiệu nào" : String.join(" · ", parts);
        return "Điểm " + row.matchScoreText() + "/100. " + s + ".";
    }
}

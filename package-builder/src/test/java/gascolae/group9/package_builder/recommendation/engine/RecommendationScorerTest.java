package gascolae.group9.package_builder.recommendation.engine;

import com.fasterxml.jackson.databind.JsonNode;
import gascolae.group9.package_builder.extraction.engine.Sv3Fixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tiêu chí nghiệm thu 2 (BAN_GIAO_SV1 mục 10): cùng một phiếu, match_score và 5 điểm thành phần lệch ≤ 0.01,
 * shortlist và thứ tự giống score.py. Chạy trên 64 phiếu: 31 phiếu bảng từ khóa, 30 phiếu Gemini thật, 3 kịch bản.
 */
class RecommendationScorerTest {
    static RecommendationScorer scorer;

    @BeforeAll
    static void init() throws Exception {
        scorer = new RecommendationScorer(MatchingRules.from(Sv3Fixtures.load("/ai/matching_rules.json")), SeedCatalog.load());
    }

    @Test
    void matchesScorePy() throws Exception {
        JsonNode cases = Sv3Fixtures.load("/sv3_reference/score_cases.json");
        assertEquals(64, cases.size());
        for (JsonNode c : cases) {
            String id = c.get("id").asText();
            JsonNode q = c.get("requirement");
            RecommendationScorer.Result res = scorer.score(RecommendationScorer.Requirement.of(
                    Sv3Fixtures.strings(q.get("objective")), Sv3Fixtures.strings(q.get("environment")),
                    Sv3Fixtures.strings(q.get("industry")), Sv3Fixtures.strings(q.get("topic")),
                    Sv3Fixtures.strings(q.get("expected_output"))));
            JsonNode rows = c.get("rows");
            assertEquals(rows.size(), res.all().size(), id);
            for (int i = 0; i < rows.size(); i++) {
                JsonNode e = rows.get(i);
                RecommendationScorer.ScoreRow g = res.all().get(i);
                String at = id + " #" + i;
                assertEquals(e.get("service_code").asText(), g.getServiceCode(), at);
                assertEquals(e.get("match_score").asDouble(), g.getMatchScore(), 0.01, at);
                near(e.get("objective_score"), g.getObjectiveScore(), at + " objective");
                near(e.get("use_case_score"), g.getUseCaseScore(), at + " use_case");
                near(e.get("industry_score"), g.getIndustryScore(), at + " industry");
                near(e.get("output_score"), g.getOutputScore(), at + " output");
                near(e.get("tag_score"), g.getTagScore(), at + " tag");
                assertEquals(e.get("reason").asText(), g.getReason(), at + " reason");
            }
            assertEquals(Sv3Fixtures.strings(c.get("shortlist")),
                    res.shortlist().stream().map(RecommendationScorer.ScoreRow::getServiceCode).toList(), id + " shortlist");
        }
    }

    @Test
    void noObjectiveStillScoresButOutOfScopeHasNoShortlist() {
        RecommendationScorer.Result res = scorer.score(new RecommendationScorer.Requirement(Map.of()));
        assertEquals(List.of(), res.shortlist());
        res.all().forEach(r -> assertNull(r.getObjectiveScore()));
    }

    private static void near(JsonNode expected, Double got, String msg) {
        if (expected == null || expected.isNull()) {
            assertNull(got, msg);
        } else {
            assertEquals(expected.asDouble(), got, 0.01, msg);
        }
    }
}

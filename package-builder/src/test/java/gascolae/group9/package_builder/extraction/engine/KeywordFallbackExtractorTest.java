package gascolae.group9.package_builder.extraction.engine;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Lớp bảng từ khóa phải cho đúng mã như fallback_extract.py trên 30 câu thử + form mẫu. */
class KeywordFallbackExtractorTest {
    static KeywordFallbackExtractor extractor;

    @BeforeAll
    static void init() throws Exception {
        extractor = new KeywordFallbackExtractor(Sv3Fixtures.keywords());
    }

    @Test
    void matchesPythonReference() throws Exception {
        JsonNode cases = Sv3Fixtures.load("/sv3_reference/fallback_cases.json");
        assertEquals(31, cases.size());
        for (JsonNode c : cases) {
            String id = c.get("id").asText();
            ExtractionResult got = extractor.extract(c.get("text").asText());
            JsonNode exp = c.get("result");
            for (String field : ExtractionResult.CODE_FIELDS) {
                assertEquals(Sv3Fixtures.strings(exp.get(field)), got.codes(field), id + " " + field);
            }
            if (exp.get("area_value").isNull()) {
                assertNull(got.getAreaValue(), id + " area");
            } else {
                assertEquals(exp.get("area_value").asDouble(), got.getAreaValue(), 1e-9, id + " area");
                assertEquals(exp.get("area_unit").asText(), got.getAreaUnit(), id + " unit");
            }
            assertEquals(KeywordFallbackExtractor.FALLBACK_OVERALL, got.overall());
            assertEquals(ConfidencePolicy.Decision.NEED_CONFIRM == ConfidencePolicy.decide(got).decision()
                    || got.codes(ExtractionResult.OBJECTIVE).isEmpty(), true, id + " fallback luôn cần xác nhận");
        }
    }
}

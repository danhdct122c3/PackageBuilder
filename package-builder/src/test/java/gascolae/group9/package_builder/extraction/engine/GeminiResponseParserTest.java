package gascolae.group9.package_builder.extraction.engine;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeminiResponseParserTest {

    @Test
    void skipsThoughtsUsageAndInput() throws Exception {
        JsonNode resp = Sv3Fixtures.MAPPER.readTree("""
                {"id":"x","input":[{"type":"text","text":"Câu nhu cầu"}],
                 "outputs":[{"type":"thought","text":"đang nghĩ"},
                            {"type":"text","text":"```json\\n{\\"objective_codes\\":[\\"CARBON_CREDIT_MRV\\"]}\\n```"}],
                 "usage":{"text":"bỏ qua"}}
                """);
        String text = GeminiResponseParser.outputText(resp);
        assertEquals("CARBON_CREDIT_MRV",
                GeminiResponseParser.parseJson(text, Sv3Fixtures.MAPPER).get("objective_codes").get(0).asText());
    }

    @Test
    void noTextThrows() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> GeminiResponseParser.outputText(Sv3Fixtures.MAPPER.readTree("{\"usage\":{\"total\":1}}")));
    }
}

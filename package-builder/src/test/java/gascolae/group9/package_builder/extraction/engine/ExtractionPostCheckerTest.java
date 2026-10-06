package gascolae.group9.package_builder.extraction.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tiêu chí nghiệm thu 1 (BAN_GIAO_SV1 mục 10): cùng JSON Gemini → mảng mã sau hậu kiểm giống hệt bản Python. */
class ExtractionPostCheckerTest {

    @Test
    void matchesPythonPostcheck() throws Exception {
        JsonNode schema = Sv3Fixtures.load("/ai/extraction_schema.json");
        ExtractionPostChecker checker = new ExtractionPostChecker(schema);
        assertEquals(6, checker.getAllowed().size());
        int i = 0;
        for (JsonNode c : Sv3Fixtures.load("/sv3_reference/postcheck_cases.json")) {
            ObjectNode data = c.get("raw").deepCopy();
            List<String> warnings = checker.postcheck(data);
            c.get("result").fields().forEachRemaining(e ->
                    assertEquals(Sv3Fixtures.strings(e.getValue()), Sv3Fixtures.strings(data.get(e.getKey())), e.getKey()));
            List<String> nonSchema = warnings.stream().filter(w -> !w.startsWith("Sai schema")).toList();
            assertEquals(Sv3Fixtures.strings(c.get("warnings")), nonSchema, "case " + i++);
        }
    }

    @Test
    void schemaErrorIsWarningNotFailure() throws Exception {
        ExtractionPostChecker checker = new ExtractionPostChecker(Sv3Fixtures.load("/ai/extraction_schema.json"));
        ObjectNode data = (ObjectNode) Sv3Fixtures.MAPPER.readTree(
                "{\"objective_codes\":[\"CARBON_CREDIT_MRV\"],\"area_unit\":\"mẫu\",\"confidence\":{\"overall\":0.9}}");
        List<String> w = checker.postcheck(data);
        assertTrue(w.stream().anyMatch(x -> x.startsWith("Sai schema")), w.toString());
        assertEquals(List.of("CARBON_CREDIT_MRV"), ExtractionPostChecker.toResult(data).codes(ExtractionResult.OBJECTIVE));
    }

    @Test
    void outOfScopeIsInsufficient() throws Exception {
        ObjectNode data = (ObjectNode) Sv3Fixtures.MAPPER.readTree(
                "{\"objective_codes\":[],\"confidence\":{\"overall\":0.2},\"notes\":\"Nhu cầu ngoài phạm vi dịch vụ hiện có: khảo sát đê\"}");
        ExtractionResult r = ExtractionPostChecker.toResult(data);
        assertEquals(ConfidencePolicy.Decision.INSUFFICIENT, ConfidencePolicy.decide(r).decision());
        r.setCodes(ExtractionResult.OBJECTIVE, List.of("CARBON_CREDIT_MRV"));
        r.getConfidence().put("overall", 0.8);
        assertEquals(ConfidencePolicy.Decision.HIGH, ConfidencePolicy.decide(r).decision());
        r.getConfidence().put("overall", 0.5);
        assertEquals(ConfidencePolicy.Decision.NEED_CONFIRM, ConfidencePolicy.decide(r).decision());
        r.getConfidence().put("overall", 0.39);
        assertEquals(ConfidencePolicy.Decision.INSUFFICIENT, ConfidencePolicy.decide(r).decision());
    }
}

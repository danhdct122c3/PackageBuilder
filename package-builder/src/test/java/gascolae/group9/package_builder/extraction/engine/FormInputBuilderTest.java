package gascolae.group9.package_builder.extraction.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Dựng input từ form phải giống form_extract.py và không bao giờ chứa thông tin cá nhân. */
class FormInputBuilderTest {

    @Test
    void matchesPythonFormSplit() throws Exception {
        for (JsonNode c : Sv3Fixtures.load("/sv3_reference/form_cases.json")) {
            Map<String, Object> form = Sv3Fixtures.MAPPER.convertValue(c.get("form"), new TypeReference<>() {
            });
            FormInputBuilder.FormInput in = FormInputBuilder.build(form);
            assertEquals(c.get("ai_text").asText(), in.aiText());
            assertEquals(c.get("kw_text").asText(), in.keywordText());
            assertEquals(Sv3Fixtures.strings(c.get("sent")), in.fieldsSentToAi());
            assertEquals(c.get("user_input").asText(), in.userInput());
            for (String pii : new String[]{"contactEmail", "contactPhone", "customerName"}) {
                Object v = form.get(pii);
                if (v instanceof String s && !s.isBlank()) {
                    assertFalse(in.userInput().contains(s), pii + " lọt vào input AI");
                }
            }
            FormInputBuilder.Area area = FormInputBuilder.parseArea(form.get("areaValue"));
            if (c.get("area_value").isNull()) {
                assertNull(area.value());
            } else {
                assertEquals(c.get("area_value").asDouble(), area.value(), 1e-9);
            }
            assertEquals(c.get("area_unit").isNull() ? null : c.get("area_unit").asText(), area.unit());
            assertEquals(c.get("area_warning").isNull() ? null : c.get("area_warning").asText(), area.warning());
        }
    }
}

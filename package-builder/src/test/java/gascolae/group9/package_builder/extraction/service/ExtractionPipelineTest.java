package gascolae.group9.package_builder.extraction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import gascolae.group9.package_builder.extraction.config.GeminiProperties;
import gascolae.group9.package_builder.extraction.engine.ConfidencePolicy;
import gascolae.group9.package_builder.extraction.engine.ExtractionResult;
import gascolae.group9.package_builder.extraction.engine.KeywordFallbackExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Luồng 3 lớp của D6 với một "Gemini giả" chạy trên máy (không gọi mạng thật):
 * request đúng định dạng, thử lại khi 503, chuyển model, rơi xuống bảng từ khóa, che key trong lỗi.
 */
class ExtractionPipelineTest {
    static final String KEY = "AIzaTEST-secret-key-1234567890";
    static final String TEXT = "Bên mình quản lý 2.000 ha rừng trồng, muốn đánh giá trữ lượng carbon và chuẩn bị hồ sơ MRV";
    static AiResources resources;
    HttpServer server;
    final List<JsonNode> requests = new ArrayList<>();

    @BeforeAll
    static void load() {
        resources = new AiResources();
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Mỗi phần tử: {status, body}. Request thứ i nhận phản hồi thứ i (hết danh sách thì lặp lại phần tử cuối). */
    private GeminiProperties fakeGemini(String[][] replies) throws Exception {
        AtomicInteger n = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1beta/interactions", ex -> {
            requests.add(resources.getMapper().readTree(ex.getRequestBody()));
            assertEquals(KEY, ex.getRequestHeaders().getFirst("x-goog-api-key"));
            String[] r = replies[Math.min(n.getAndIncrement(), replies.length - 1)];
            byte[] body = r[1].getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(Integer.parseInt(r[0]), body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        GeminiProperties p = new GeminiProperties();
        p.setApiKey(KEY);
        p.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta/interactions");
        p.setModels(List.of("model-chinh", "model-du-phong"));
        p.setRetryWaitSeconds(0);
        p.setTimeoutSeconds(5);
        return p;
    }

    private ExtractionPipeline pipeline(GeminiProperties p) {
        return new ExtractionPipeline(p, new GeminiClient(p, resources), resources);
    }

    private static String geminiReply(String json) throws Exception {
        String quoted = resources.getMapper().writeValueAsString(json);
        return "{\"outputs\":[{\"type\":\"thought\",\"text\":\"...\"},{\"type\":\"text\",\"text\":" + quoted + "}],"
                + "\"usage\":{\"total_tokens\":10}}";
    }

    static final String GOOD = """
            {"project_name":null,"objective_raw":"đánh giá trữ lượng carbon và chuẩn bị hồ sơ MRV",
             "industry_raw":null,"environment_raw":"rừng trồng","area_value":2000,"area_unit":"ha",
             "objective_codes":["CARBON_STOCK_ASSESSMENT","CARBON_CREDIT_MRV","CARBON_STOCK_ASSESSMENT"],
             "environment_codes":["FOREST"],"industry_codes":[],"topic_codes":["carbon","mrv"],
             "expected_output_codes":["AGB_CARBON_STOCK_MAP","MRV_REPORT","MA_BIA"],"available_input_codes":[],
             "confidence":{"objective":0.95,"environment":0.9,"industry":0,"expected_output":0.8,"topic":0.9,"overall":0.86},
             "notes":null}""";

    @Test
    void geminiOkRequestFormatAndPostcheck() throws Exception {
        ExtractionResult r = pipeline(fakeGemini(new String[][]{{"200", geminiReply(GOOD)}})).extractText(TEXT);
        assertEquals("model-chinh", r.getLayer());
        assertEquals(List.of("CARBON_STOCK_ASSESSMENT", "CARBON_CREDIT_MRV"), r.codes(ExtractionResult.OBJECTIVE));
        assertEquals(List.of("AGB_CARBON_STOCK_MAP", "MRV_REPORT"), r.codes(ExtractionResult.EXPECTED_OUTPUT));
        assertTrue(r.getWarnings().stream().anyMatch(w -> w.startsWith("Gộp mã trùng ở objective_codes")));
        assertTrue(r.getWarnings().stream().anyMatch(w -> w.equals("Loại mã không hợp lệ ở expected_output_codes: ['MA_BIA']")));
        assertEquals(ConfidencePolicy.Decision.HIGH, ConfidencePolicy.decide(r).decision());
        assertEquals(2000.0, r.getAreaValue());

        JsonNode body = requests.get(0);
        assertEquals("model-chinh", body.get("model").asText());
        assertFalse(body.get("store").asBoolean());
        assertEquals(0, body.get("generation_config").get("temperature").asInt());
        assertEquals("application/json", body.get("response_format").get("mime_type").asText());
        assertEquals("Câu nhu cầu của khách:\n\n" + TEXT + "\n\nTrả về đúng một object JSON, không thêm chữ nào ngoài JSON.",
                body.get("input").asText());
        String system = body.get("system_instruction").asText();
        assertTrue(system.startsWith("# Prompt trích xuất") && system.contains("Bạn là bộ trích xuất"), "giống load_prompt_parts()");
        assertFalse(system.contains("## User"));
        assertEquals("v1.4", resources.getPromptVersion());
    }

    @Test
    void retry503ThenSwitchModel() throws Exception {
        ExtractionResult r = pipeline(fakeGemini(new String[][]{
                {"503", "{\"error\":\"overloaded " + KEY + "\"}"},
                {"503", "{\"error\":\"overloaded " + KEY + "\"}"},
                {"200", geminiReply(GOOD)}})).extractText(TEXT);
        assertEquals(3, requests.size(), "model chính: 1 lần + 1 lần thử lại, rồi model dự phòng");
        assertEquals("model-du-phong", r.getLayer());
        assertEquals(1, r.getPreviousErrors().size());
        assertTrue(r.getPreviousErrors().get(0).startsWith("model-chinh: HTTP 503"));
        assertFalse(r.getPreviousErrors().get(0).contains(KEY), "key phải được che trong lỗi");
    }

    @Test
    void bothModelsFailFallsBackToKeywords() throws Exception {
        ExtractionResult r = pipeline(fakeGemini(new String[][]{{"400", "{\"error\":\"bad request\"}"}})).extractText(TEXT);
        assertEquals(2, requests.size(), "400 không thử lại");
        assertEquals(KeywordFallbackExtractor.FALLBACK_OVERALL, r.overall());
        assertTrue(r.isFallback());
        assertEquals(2, r.getPreviousErrors().size());
        assertTrue(r.codes(ExtractionResult.OBJECTIVE).contains("CARBON_CREDIT_MRV"));
        assertEquals(ConfidencePolicy.Decision.NEED_CONFIRM, ConfidencePolicy.decide(r).decision());
    }

    @Test
    void noKeyOrNoNetworkNeverThrows() {
        GeminiProperties noKey = new GeminiProperties();
        ExtractionResult r = pipeline(noKey).extractText(TEXT);
        assertTrue(r.isFallback());
        assertTrue(r.getPreviousErrors().get(0).contains("GEMINI_API_KEY"));

        GeminiProperties offline = new GeminiProperties();
        offline.setApiKey(KEY);
        offline.setEndpoint("http://127.0.0.1:9/v1beta/interactions");
        offline.setTimeoutSeconds(2);
        ExtractionResult r2 = pipeline(offline).extractText(TEXT);
        assertTrue(r2.isFallback());
        assertEquals(2, r2.getPreviousErrors().size());
        r2.getPreviousErrors().forEach(e -> assertFalse(e.contains(KEY)));
    }

    @Test
    void quotedKeyFromEnvFileIsStripped() throws Exception {
        GeminiProperties p = fakeGemini(new String[][]{{"200", geminiReply(GOOD)}});
        p.setApiKey(" \"" + KEY + "\" ");
        ExtractionResult r = pipeline(p).extractText(TEXT);
        assertEquals("model-chinh", r.getLayer(), "server giả kiểm header x-goog-api-key đúng bằng KEY, không kèm dấu nháy");
    }

    @Test
    void garbageResponseIsAnErrorNotACrash() throws Exception {
        ExtractionResult r = pipeline(fakeGemini(new String[][]{{"200", "{\"usage\":{}}"}})).extractText(TEXT);
        assertTrue(r.isFallback());
        assertTrue(r.getPreviousErrors().get(0).contains("Không tìm thấy phần chữ"));
    }
}

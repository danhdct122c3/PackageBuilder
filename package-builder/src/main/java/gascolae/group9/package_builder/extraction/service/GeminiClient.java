package gascolae.group9.package_builder.extraction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gascolae.group9.package_builder.extraction.config.GeminiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * Gọi Gemini Interactions API bằng REST (port post() trong gemini_extract.py).
 * HTTP 429/500/503 → chờ retryWaitSeconds rồi thử lại đúng 1 lần cùng model; lỗi khác ném ra để chuyển model.
 */
@Component
@Slf4j
public class GeminiClient {
    private static final Set<Integer> RETRY_CODES = Set.of(429, 500, 503);

    private final GeminiProperties props;
    private final ObjectMapper mapper;
    private final HttpClient http;

    public GeminiClient(GeminiProperties props, AiResources resources) {
        this.props = props;
        this.mapper = resources.getMapper();
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    /** Body chuẩn cho một lần trích xuất (BAN_GIAO_SV1 mục 5). */
    public ObjectNode extractionBody(String model, String systemInstruction, String input, JsonNode schema) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("store", false); // Google không lưu hội thoại chứa nhu cầu khách
        body.put("system_instruction", systemInstruction);
        body.put("input", input);
        body.putObject("generation_config").put("temperature", 0);
        ObjectNode rf = body.putObject("response_format");
        rf.put("type", "text");
        rf.put("mime_type", "application/json");
        rf.set("schema", schema);
        return body;
    }

    public ObjectNode pingBody(String model) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("store", false);
        body.put("input", "Trả lời đúng một từ: OK");
        return body;
    }

    public JsonNode post(ObjectNode body) throws IOException, InterruptedException {
        try {
            return send(body);
        } catch (GeminiException e) {
            if (!RETRY_CODES.contains(e.getStatus())) {
                throw e;
            }
            log.warn("Gemini {} trả HTTP {}, chờ {}s rồi thử lại 1 lần", body.path("model").asText(), e.getStatus(),
                    props.getRetryWaitSeconds());
            Thread.sleep(props.getRetryWaitSeconds() * 1000L);
            return send(body);
        }
    }

    private JsonNode send(ObjectNode body) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(props.getEndpoint()))
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", props.getApiKey().trim())
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            String detail = props.mask(resp.body());
            if (detail != null && detail.length() > 300) {
                detail = detail.substring(0, 300);
            }
            throw new GeminiException(resp.statusCode(), "HTTP " + resp.statusCode() + " " + detail);
        }
        return mapper.readTree(resp.body());
    }
}

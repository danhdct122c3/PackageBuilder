package gascolae.group9.package_builder.extraction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gascolae.group9.package_builder.extraction.engine.ExtractionPostChecker;
import gascolae.group9.package_builder.extraction.engine.FormInputBuilder;
import gascolae.group9.package_builder.extraction.engine.GeminiSchemaCleaner;
import gascolae.group9.package_builder.extraction.engine.KeywordFallbackExtractor;
import gascolae.group9.package_builder.recommendation.engine.MatchingRules;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nạp một lần lúc khởi động các file cấu hình AI của SV3 (src/main/resources/ai):
 * prompt D6 v1.4, schema kiểm tra + schema gửi Gemini, bảng từ khóa dự phòng, matching_rules D4.
 * Muốn đổi prompt/trọng số: thay file trong thư mục ai/ (chỉ khi SV3 đồng ý) rồi khởi động lại.
 *
 * Dùng ObjectMapper Jackson 2 riêng cho cây JSON động (schema, response Gemini), độc lập với Jackson của Spring MVC.
 */
@Component
@Getter
@Slf4j
public class AiResources {
    private static final Pattern PROMPT_VERSION = Pattern.compile("prompt (v\\d+(?:\\.\\d+)*)");

    private final ObjectMapper mapper = new ObjectMapper();
    private final String systemPrompt;
    private final String promptVersion;
    private final JsonNode fullSchema;
    private final JsonNode geminiSchema;
    private final JsonNode formSchema;
    private final JsonNode formGeminiSchema;
    private final ExtractionPostChecker fullChecker;
    private final ExtractionPostChecker formChecker;
    private final KeywordFallbackExtractor fallback;
    private final MatchingRules matchingRules;

    public AiResources() {
        String md = text("ai/extraction_prompt.md");
        // Giống load_prompt_parts(): lấy phần trước "## User", bỏ dòng tiêu đề "## System".
        this.systemPrompt = md.split("## User", 2)[0].replaceFirst("## System", "").strip();
        Matcher m = PROMPT_VERSION.matcher(systemPrompt);
        this.promptVersion = m.find() ? m.group(1) : "unknown";
        this.fullSchema = json("ai/extraction_schema.json");
        this.geminiSchema = json("ai/extraction_schema.gemini.json");
        this.formSchema = FormInputBuilder.formSchema(fullSchema);
        this.formGeminiSchema = GeminiSchemaCleaner.clean(formSchema);
        this.fullChecker = new ExtractionPostChecker(fullSchema);
        this.formChecker = new ExtractionPostChecker(formSchema);
        Map<String, Map<String, List<String>>> keywords;
        try (InputStream is = new ClassPathResource("ai/keywords.json").getInputStream()) {
            keywords = mapper.readValue(is, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.fallback = new KeywordFallbackExtractor(keywords);
        this.matchingRules = MatchingRules.from(json("ai/matching_rules.json"));
        log.info("Đã nạp cấu hình AI: prompt {} ({} ký tự), matching rules {}",
                promptVersion, systemPrompt.length(), matchingRules.version());
    }

    private static String text(String path) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được " + path, e);
        }
    }

    private JsonNode json(String path) {
        try {
            return mapper.readTree(text(path));
        } catch (IOException e) {
            throw new UncheckedIOException("JSON lỗi trong " + path, e);
        }
    }
}

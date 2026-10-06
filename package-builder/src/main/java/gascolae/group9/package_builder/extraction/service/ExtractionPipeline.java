package gascolae.group9.package_builder.extraction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gascolae.group9.package_builder.extraction.config.GeminiProperties;
import gascolae.group9.package_builder.extraction.engine.ExtractionPostChecker;
import gascolae.group9.package_builder.extraction.engine.ExtractionResult;
import gascolae.group9.package_builder.extraction.engine.GeminiResponseParser;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * Luồng 3 lớp của D6 (port extract() trong gemini_extract.py):
 * model chính → model dự phòng → bảng từ khóa; mỗi lớp AI đều qua hậu kiểm.
 * Không bao giờ ném lỗi ra ngoài: rút key hoặc mất mạng vẫn trả phiếu bằng bảng từ khóa (tiêu chí nghiệm thu 4).
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ExtractionPipeline {
    public static final String WRAP_PREFIX = "Câu nhu cầu của khách:\n\n";
    public static final String WRAP_SUFFIX = "\n\nTrả về đúng một object JSON, không thêm chữ nào ngoài JSON.";

    GeminiProperties props;
    GeminiClient client;
    AiResources resources;

    /** Câu nhu cầu tự do (Sales gõ hoặc dán). */
    public ExtractionResult extractText(String text) {
        return run(WRAP_PREFIX + text + WRAP_SUFFIX, resources.getFullChecker(), resources.getGeminiSchema(), text);
    }

    /** Chế độ form: userInput đã dựng sẵn bởi FormInputBuilder, keywordText là các giá trị thô cho bảng từ khóa. */
    public ExtractionResult extractForm(String userInput, String keywordText) {
        return run(userInput, resources.getFormChecker(), resources.getFormGeminiSchema(), keywordText);
    }

    private ExtractionResult run(String userInput, ExtractionPostChecker checker, JsonNode geminiSchema, String fallbackText) {
        List<String> errors = new ArrayList<>();
        if (!props.isConfigured()) {
            errors.add("Chưa cấu hình GEMINI_API_KEY: bỏ qua Gemini, dùng bảng từ khóa");
        } else {
            for (String model : props.getModels()) {
                long t0 = System.currentTimeMillis();
                try {
                    JsonNode resp = client.post(
                            client.extractionBody(model, resources.getSystemPrompt(), userInput, geminiSchema));
                    ObjectNode data = GeminiResponseParser.parseJson(GeminiResponseParser.outputText(resp), resources.getMapper());
                    List<String> warnings = checker.postcheck(data);
                    ExtractionResult r = ExtractionPostChecker.toResult(data);
                    r.setLayer(model);
                    r.setLatencyMs(System.currentTimeMillis() - t0);
                    r.getWarnings().addAll(warnings);
                    r.getPreviousErrors().addAll(errors);
                    log.info("D6 trích xuất bằng {} trong {} ms, overall={}, cảnh báo={}",
                            model, r.getLatencyMs(), r.overall(), warnings);
                    return r;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.add(model + ": bị ngắt");
                    break;
                } catch (HttpTimeoutException e) {
                    errors.add(model + ": timeout sau " + props.getTimeoutSeconds() + " giây");
                } catch (Exception e) {
                    String msg = props.mask(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                    errors.add(model + ": " + (msg.length() > 300 ? msg.substring(0, 300) : msg));
                }
                log.warn("D6 lỗi ở {}: {}", model, errors.get(errors.size() - 1));
            }
        }
        ExtractionResult r = resources.getFallback().extract(fallbackText);
        r.getPreviousErrors().addAll(errors);
        log.warn("D6 chuyển sang bảng từ khóa. Lỗi trước đó: {}", errors);
        return r;
    }

    /** Gọi thử từng model với một câu ngắn. Trả model đầu tiên trả lời được, hoặc lỗi của từng model. */
    public List<String> ping() {
        List<String> lines = new ArrayList<>();
        if (!props.isConfigured()) {
            lines.add("Chưa cấu hình GEMINI_API_KEY");
            return lines;
        }
        for (String model : props.getModels()) {
            try {
                String answer = GeminiResponseParser.outputText(client.post(client.pingBody(model))).strip();
                lines.add(model + ": kết nối được, trả lời \"" + answer + "\"");
                return lines;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                lines.add(model + ": bị ngắt");
                return lines;
            } catch (Exception e) {
                lines.add(model + ": " + props.mask(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            }
        }
        return lines;
    }
}

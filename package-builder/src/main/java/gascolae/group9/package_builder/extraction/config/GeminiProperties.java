package gascolae.group9.package_builder.extraction.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Cấu hình gọi Gemini (BAN_GIAO_SV1 mục 5). API key CHỈ đọc từ biến môi trường GEMINI_API_KEY
 * (hoặc file .env cạnh nơi chạy app), không bao giờ ghi vào code, application.yaml hay git.
 */
@Component
@ConfigurationProperties(prefix = "gemini")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeminiProperties {
    /** Đọc từ ${GEMINI_API_KEY}. Rỗng → hệ thống chạy thẳng lớp bảng từ khóa. */
    String apiKey;

    String endpoint = "https://generativelanguage.googleapis.com/v1beta/interactions";

    /** Thứ tự thử: model chính → model dự phòng → bảng từ khóa. */
    List<String> models = new ArrayList<>(List.of("gemini-3.8-flash", "gemini-3.1-flash-lite"));

    /** 60 giây mỗi lần gọi (30 giây làm nhiều câu bị timeout). */
    int timeoutSeconds = 60;

    /** HTTP 429/500/503 → chờ rồi thử lại 1 lần cùng model, sau đó mới đổi model. */
    int retryWaitSeconds = 3;

    /**
     * Bỏ khoảng trắng và dấu nháy bao quanh: file .env đọc dạng .properties nên
     * GEMINI_API_KEY="..." sẽ giữ nguyên dấu nháy và Google báo API_KEY_INVALID.
     */
    public void setApiKey(String apiKey) {
        String v = apiKey == null ? null : apiKey.strip();
        if (v != null && v.length() >= 2
                && (v.startsWith("\"") && v.endsWith("\"") || v.startsWith("'") && v.endsWith("'"))) {
            v = v.substring(1, v.length() - 1).strip();
        }
        this.apiKey = v;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    /** Che key trong mọi thông báo lỗi và log. */
    public String mask(String text) {
        if (text == null || !isConfigured()) {
            return text;
        }
        String key = apiKey.trim();
        String masked = key.length() > 6 ? key.substring(0, 4) + "…" + key.substring(key.length() - 2) : "***";
        return text.replace(key, masked);
    }
}

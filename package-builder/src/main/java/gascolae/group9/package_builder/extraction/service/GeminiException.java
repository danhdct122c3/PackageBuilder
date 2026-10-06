package gascolae.group9.package_builder.extraction.service;

import lombok.Getter;

/** Lỗi HTTP từ Gemini. Thông điệp đã được che key trước khi tạo exception. */
@Getter
public class GeminiException extends RuntimeException {
    private final int status;

    public GeminiException(int status, String message) {
        super(message);
        this.status = status;
    }
}

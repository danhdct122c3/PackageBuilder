package gascolae.group9.package_builder.extraction.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Chạy D6 cho một requirement đang DRAFT.
 * - Có {@code text}: trích xuất câu này và lưu làm raw_requirement_text.
 * - Không có: dùng raw_requirement_text đã lưu; nếu cũng trống thì dựng input từ các trường form (chế độ form).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractRequirementRequest {
    @Size(max = 5000, message = "INVALID_REQUEST")
    String text;
}

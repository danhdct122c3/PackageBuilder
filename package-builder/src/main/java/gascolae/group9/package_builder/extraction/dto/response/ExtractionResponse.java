package gascolae.group9.package_builder.extraction.dto.response;

import gascolae.group9.package_builder.customer.dto.response.RequirementResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Phiếu trích xuất D6 sau hậu kiểm + quyết định theo ngưỡng tin cậy. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractionResponse {
    /** TEXT (câu tự do) hoặc FORM (dựng từ các trường form). */
    String mode;
    /** Lớp đã chạy: tên model Gemini hoặc FALLBACK_KEYWORD. Chỉ để hiển thị/log. */
    String layer;
    /** HIGH | NEED_CONFIRM | INSUFFICIENT. */
    String decision;
    String decisionMessage;
    /** Được chạy gợi ý D4 sau khi Sales xác nhận hay chưa (false khi INSUFFICIENT). */
    boolean recommendationAllowed;
    /** true khi notes bắt đầu bằng "Nhu cầu ngoài phạm vi dịch vụ hiện có:" → hiện nổi bật cho Sales. */
    boolean outOfScope;

    String projectName;
    String objectiveRaw;
    String industryRaw;
    String environmentRaw;
    BigDecimal areaValue;
    String areaUnit;

    List<CodeLabel> objectiveCodes;
    List<CodeLabel> environmentCodes;
    List<CodeLabel> industryCodes;
    List<CodeLabel> topicCodes;
    List<CodeLabel> expectedOutputCodes;
    /** Không có bảng riêng trong DB V2: chỉ trả về để Validation Engine dùng khi kiểm MISSING_REQUIRED_INPUT. */
    List<CodeLabel> availableInputCodes;

    Map<String, Double> confidence;
    BigDecimal extractionConfidence;
    String notes;
    List<String> warnings;
    List<String> previousErrors;
    Long latencyMs;
    String promptVersion;

    /** Có khi chạy cho một requirement và đã lưu vào DB. */
    RequirementResponse requirement;
}

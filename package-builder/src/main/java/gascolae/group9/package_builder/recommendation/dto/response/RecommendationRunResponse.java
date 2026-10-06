package gascolae.group9.package_builder.recommendation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

/** Kết quả một lần chạy D4. ran = false khi phiếu không có mục tiêu (không đủ căn cứ, không sinh dòng nào). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationRunResponse {
    String requirementId;
    String requirementCode;
    boolean ran;
    String message;
    /** 5 mảng mã đã dùng để chấm. */
    Map<String, List<String>> requirementCodes;
    int servicesScored;
    String matchingRulesVersion;
    List<RecommendationResponse> recommendations;
}

package gascolae.group9.package_builder.extraction.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/** Các mã đang lưu của một requirement (requirement_tags + requirement_expected_outputs có data_item_id). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequirementCodesResponse {
    String requirementId;
    String requirementCode;
    String status;
    String extractionMethod;
    BigDecimal extractionConfidence;
    List<CodeLabel> objectiveCodes;
    List<CodeLabel> environmentCodes;
    List<CodeLabel> industryCodes;
    List<CodeLabel> topicCodes;
    List<CodeLabel> expectedOutputCodes;
    List<String> warnings;
}

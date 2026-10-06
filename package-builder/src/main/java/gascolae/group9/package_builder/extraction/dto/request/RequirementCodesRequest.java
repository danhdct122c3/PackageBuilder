package gascolae.group9.package_builder.extraction.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Sales sửa phiếu trước khi xác nhận: thay toàn bộ mã của requirement bằng các danh sách này. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequirementCodesRequest {
    List<String> objectiveCodes;
    List<String> environmentCodes;
    List<String> industryCodes;
    List<String> topicCodes;
    List<String> expectedOutputCodes;
}

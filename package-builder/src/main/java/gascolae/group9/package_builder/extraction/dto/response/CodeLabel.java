package gascolae.group9.package_builder.extraction.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Mã kèm tên tiếng Việt để hiển thị trên phiếu. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CodeLabel {
    String code;
    String name;
}

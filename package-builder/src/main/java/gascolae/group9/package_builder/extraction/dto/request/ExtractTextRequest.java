package gascolae.group9.package_builder.extraction.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Câu nhu cầu tiếng Việt Sales nhập. Chỉ gửi câu nhu cầu, không gửi tên/email/số điện thoại khách. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractTextRequest {
    @NotBlank(message = "NOT_NULL")
    @Size(max = 5000, message = "INVALID_REQUEST")
    String text;
}

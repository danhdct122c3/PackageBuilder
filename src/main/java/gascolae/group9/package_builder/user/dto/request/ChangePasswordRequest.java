package gascolae.group9.package_builder.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest {
    @NotBlank(message = "NOT_BLANK")
    String oldPassword;
    @NotBlank(message = "NOT_BLANK")
    String newPassword;
    @NotBlank(message = "NOT_BLANK")
    String confirmPassword;
}

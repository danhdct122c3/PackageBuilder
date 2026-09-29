package gascolae.group9.package_builder.user.dto.response;


import gascolae.group9.package_builder.user.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Data  //@Data = @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String userId;
    String username;
    String email;
    String fullName;
    UserStatus userStatus;


    Set<RoleResponse> role;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

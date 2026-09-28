package gascolae.group9.package_builder.user.dto.response;

import gascolae.group9.package_builder.user.entity.Role;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
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
    String password;
    String fullName;
    boolean active;


    Set<Role> role;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

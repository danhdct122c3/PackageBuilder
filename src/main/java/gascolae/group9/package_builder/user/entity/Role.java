package gascolae.group9.package_builder.user.entity;
import gascolae.group9.package_builder.user.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Role {
    @Id @Enumerated(EnumType.STRING)
    RoleName name;
    String description;

}

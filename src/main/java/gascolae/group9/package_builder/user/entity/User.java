package gascolae.group9.package_builder.user.entity;

import gascolae.group9.package_builder.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String userId;
    String username;
    String email;
    String password;
    String fullName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(
                    name = "role_name",
                    referencedColumnName = "name"
            )
    )
    Set<Role> role;

    @Enumerated(EnumType.STRING)
    UserStatus userStatus;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;
    @LastModifiedDate
    @Column(nullable = false)
    LocalDateTime updatedAt;
}

package gascolae.group9.package_builder.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequirementTagId implements Serializable {
    @Column(name = "requirement_id")
    String requirementId;

    @Column(name = "tag_id")
    String tagId;
}

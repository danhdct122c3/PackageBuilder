package gascolae.group9.package_builder.customer.entity;

import gascolae.group9.package_builder.catalog.entity.Tag;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Bảng requirement_tags (DB V2 mục 7): chuẩn hóa Objective / Environment / Industry / Topic của Requirement.
 * D6 ghi các mã AI trích xuất vào đây; D4 đọc lại để chấm điểm.
 */
@Entity
@Table(name = "requirement_tags")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequirementTag {
    @EmbeddedId
    RequirementTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("requirementId")
    @JoinColumn(name = "requirement_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    CustomerRequirement requirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", nullable = false)
    Tag tag;
}

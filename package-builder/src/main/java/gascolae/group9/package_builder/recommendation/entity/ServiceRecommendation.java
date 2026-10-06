package gascolae.group9.package_builder.recommendation.entity;

import gascolae.group9.package_builder.catalog.entity.Service;
import gascolae.group9.package_builder.customer.entity.CustomerRequirement;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng service_recommendations (DB V2 mục 16): kết quả chấm điểm D4 cho một requirement đã xác nhận.
 * Các *_score là điểm thô 0–100 của từng tín hiệu; tín hiệu khách không nêu lưu 0 (cột NOT NULL).
 */
@Entity
@Table(
        name = "service_recommendations",
        uniqueConstraints = @UniqueConstraint(name = "uk_service_recommendations_req_service",
                columnNames = {"requirement_id", "service_id"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class ServiceRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String recommendationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    CustomerRequirement requirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    Service service;

    @Column(nullable = false, precision = 5, scale = 2)
    BigDecimal matchScore;

    @Column(nullable = false, precision = 5, scale = 2)
    BigDecimal objectiveScore;

    @Column(nullable = false, precision = 5, scale = 2)
    BigDecimal useCaseScore;

    @Column(nullable = false, precision = 5, scale = 2)
    BigDecimal industryScore;

    @Column(nullable = false, precision = 5, scale = 2)
    BigDecimal outputScore;

    @Column(nullable = false, precision = 5, scale = 2)
    BigDecimal tagScore;

    @Column(columnDefinition = "TEXT")
    String recommendationReason;

    @Column(nullable = false)
    Integer rankOrder;

    @Column(nullable = false)
    @Builder.Default
    Boolean accepted = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;
}

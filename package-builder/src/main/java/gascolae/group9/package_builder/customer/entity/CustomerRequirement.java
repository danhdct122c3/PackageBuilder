package gascolae.group9.package_builder.customer.entity;

import gascolae.group9.package_builder.customer.enums.ExtractionMethod;
import gascolae.group9.package_builder.customer.enums.RequirementStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_requirements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class CustomerRequirement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String requirementId;

    @Column(nullable = false, unique = true, length = 50)
    String requirementCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    Customer customer;

    @Column(nullable = false)
    String projectName;

    @Column(columnDefinition = "TEXT")
    String rawRequirementText;

    @Column(length = 100)
    String industryRaw;

    @Column(length = 100)
    String environmentRaw;

    @Column(precision = 14, scale = 2)
    BigDecimal areaValue;

    @Column(length = 20)
    String areaUnit;

    @Column(columnDefinition = "TEXT")
    String locationDescription;

    @Column(length = 100)
    String monitoringFrequencyRaw;

    @Column(columnDefinition = "TEXT")
    String objectiveRaw;

    @Column(columnDefinition = "TEXT")
    String providedInputsRaw;

    @Column(length = 100)
    String serviceId;

    Integer level;

    /** MANUAL (Sales tự nhập) hoặc AI (đã chạy D6). Có DEFAULT để ddl-auto thêm cột vào bảng đã có dữ liệu. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'MANUAL'")
    @Builder.Default
    ExtractionMethod extractionMethod = ExtractionMethod.MANUAL;

    /** confidence.overall của lần trích xuất gần nhất, 0–1. */
    @Column(precision = 5, scale = 4)
    BigDecimal extractionConfidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    RequirementStatus status;

    String createdBy;

    String confirmedBy;

    LocalDateTime confirmedAt;

    @OneToMany(mappedBy = "requirement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<RequirementExpectedOutput> expectedOutputs = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    LocalDateTime updatedAt;

    public void addExpectedOutput(RequirementExpectedOutput output) {
        expectedOutputs.add(output);
        output.setRequirement(this);
    }

    public void removeExpectedOutput(RequirementExpectedOutput output) {
        expectedOutputs.remove(output);
        output.setRequirement(null);
    }
}

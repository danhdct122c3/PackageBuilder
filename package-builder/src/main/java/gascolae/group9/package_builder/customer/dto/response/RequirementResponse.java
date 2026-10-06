package gascolae.group9.package_builder.customer.dto.response;

import gascolae.group9.package_builder.customer.enums.ExtractionMethod;
import gascolae.group9.package_builder.customer.enums.RequirementStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequirementResponse {
    String requirementId;
    String requirementCode;
    String customerId;
    String customerCode;
    String customerName;
    String companyName;
    String contactEmail;
    String contactPhone;
    String projectName;
    String rawRequirementText;
    String industryRaw;
    String environmentRaw;
    BigDecimal areaValue;
    String areaUnit;
    String locationDescription;
    String monitoringFrequencyRaw;
    String objectiveRaw;
    String providedInputsRaw;
    String serviceId;
    Integer level;
    ExtractionMethod extractionMethod;
    BigDecimal extractionConfidence;
    RequirementStatus status;
    String createdBy;
    String confirmedBy;
    LocalDateTime confirmedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<ExpectedOutputResponse> expectedOutputs;
}

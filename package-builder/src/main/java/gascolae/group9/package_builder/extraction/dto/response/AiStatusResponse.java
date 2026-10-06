package gascolae.group9.package_builder.extraction.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Trạng thái cấu hình AI. Không bao giờ chứa API key. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiStatusResponse {
    boolean apiKeyConfigured;
    List<String> models;
    int timeoutSeconds;
    String promptVersion;
    int promptLength;
    String matchingRulesVersion;
    List<String> ping;
}

package gascolae.group9.package_builder.extraction.service.serviceImpl;

import gascolae.group9.package_builder.catalog.entity.DataItem;
import gascolae.group9.package_builder.catalog.entity.Tag;
import gascolae.group9.package_builder.catalog.enums.TagType;
import gascolae.group9.package_builder.catalog.repository.DataItemRepository;
import gascolae.group9.package_builder.catalog.repository.TagRepository;
import gascolae.group9.package_builder.customer.entity.CustomerRequirement;
import gascolae.group9.package_builder.customer.entity.RequirementExpectedOutput;
import gascolae.group9.package_builder.customer.entity.RequirementTag;
import gascolae.group9.package_builder.customer.entity.RequirementTagId;
import gascolae.group9.package_builder.customer.enums.ExtractionMethod;
import gascolae.group9.package_builder.customer.enums.OutputPriority;
import gascolae.group9.package_builder.customer.enums.RequirementStatus;
import gascolae.group9.package_builder.customer.mapper.RequirementMapper;
import gascolae.group9.package_builder.customer.repository.CustomerRequirementRepository;
import gascolae.group9.package_builder.customer.repository.RequirementTagRepository;
import gascolae.group9.package_builder.exception.AppException;
import gascolae.group9.package_builder.exception.ErrorCode;
import gascolae.group9.package_builder.extraction.config.GeminiProperties;
import gascolae.group9.package_builder.extraction.dto.request.ExtractRequirementRequest;
import gascolae.group9.package_builder.extraction.dto.request.RequirementCodesRequest;
import gascolae.group9.package_builder.extraction.dto.response.AiStatusResponse;
import gascolae.group9.package_builder.extraction.dto.response.CodeLabel;
import gascolae.group9.package_builder.extraction.dto.response.ExtractionResponse;
import gascolae.group9.package_builder.extraction.dto.response.RequirementCodesResponse;
import gascolae.group9.package_builder.extraction.engine.ConfidencePolicy;
import gascolae.group9.package_builder.extraction.engine.ExtractionResult;
import gascolae.group9.package_builder.extraction.engine.FormInputBuilder;
import gascolae.group9.package_builder.extraction.engine.PyText;
import gascolae.group9.package_builder.extraction.service.AiResources;
import gascolae.group9.package_builder.extraction.service.ExtractionPipeline;
import gascolae.group9.package_builder.extraction.service.RequirementExtractionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RequirementExtractionServiceImpl implements RequirementExtractionService {
    static final String OUT_OF_SCOPE_PREFIX = "Nhu cầu ngoài phạm vi dịch vụ hiện có:";
    static final Map<String, TagType> FIELD_TAG_TYPE = Map.of(
            ExtractionResult.OBJECTIVE, TagType.OBJECTIVE,
            ExtractionResult.ENVIRONMENT, TagType.ENVIRONMENT,
            ExtractionResult.INDUSTRY, TagType.INDUSTRY,
            ExtractionResult.TOPIC, TagType.TOPIC);
    static final List<String> TAG_FIELDS = List.of(
            ExtractionResult.OBJECTIVE, ExtractionResult.ENVIRONMENT, ExtractionResult.INDUSTRY, ExtractionResult.TOPIC);

    ExtractionPipeline pipeline;
    AiResources resources;
    GeminiProperties geminiProperties;
    CustomerRequirementRepository requirementRepository;
    RequirementTagRepository requirementTagRepository;
    TagRepository tagRepository;
    DataItemRepository dataItemRepository;
    RequirementMapper requirementMapper;
    TransactionTemplate transactionTemplate;

    // ------------------------------------------------------------------ preview

    /** Không mở transaction trong lúc chờ Gemini (có thể tới 2 × 60 giây) để không giữ kết nối DB. */
    @Override
    public ExtractionResponse preview(String text) {
        ExtractionResult r = pipeline.extractText(text.strip());
        return transactionTemplate.execute(tx -> {
            Lookup lookup = lookup();
            r.getWarnings().addAll(lookup.dropUnknown(r));
            return toResponse(r, "TEXT", lookup, null);
        });
    }

    // ------------------------------------------------------------------ extract + lưu

    /** Đầu vào đã chuẩn bị cho AI, đọc trong một transaction ngắn. */
    private record PreparedInput(String mode, String text, FormInputBuilder.FormInput form) {
    }

    @Override
    public ExtractionResponse extractForRequirement(String requirementId, ExtractRequirementRequest request) {
        String explicitText = request == null ? null : request.getText();

        // 1) Đọc requirement (transaction ngắn).
        PreparedInput input = transactionTemplate.execute(tx -> {
            CustomerRequirement requirement = loadDraft(requirementId);
            if (StringUtils.hasText(explicitText) || StringUtils.hasText(requirement.getRawRequirementText())) {
                String text = StringUtils.hasText(explicitText) ? explicitText.strip() : requirement.getRawRequirementText();
                return new PreparedInput("TEXT", text, null);
            }
            FormInputBuilder.FormInput form = FormInputBuilder.build(formOf(requirement));
            if (form.isEmpty()) {
                throw new AppException(ErrorCode.EXTRACTION_INPUT_EMPTY);
            }
            return new PreparedInput("FORM", null, form);
        });

        // 2) Gọi AI ngoài transaction. Pipeline không ném lỗi: hỏng mạng/key thì trả phiếu bảng từ khóa.
        ExtractionResult r = "TEXT".equals(input.mode())
                ? pipeline.extractText(input.text())
                : pipeline.extractForm(input.form().userInput(), input.form().keywordText());
        if ("FORM".equals(input.mode())) {
            r.getWarnings().add("Chế độ form: đã gửi AI các trường " + input.form().fieldsSentToAi());
        }

        // 3) Lưu kết quả (transaction ngắn, đọc lại requirement và kiểm tra vẫn còn DRAFT).
        return transactionTemplate.execute(tx -> {
            CustomerRequirement requirement = loadDraft(requirementId);
            if ("TEXT".equals(input.mode())) {
                requirement.setRawRequirementText(input.text());
                applyRawFields(requirement, r);
            } else {
                // rawRequirementText = đúng đoạn AI đã đọc, để truy vết; không chứa thông tin cá nhân.
                requirement.setRawRequirementText(input.form().aiText());
            }
            Lookup lookup = lookup();
            r.getWarnings().addAll(lookup.dropUnknown(r));

            requirement.setExtractionMethod(ExtractionMethod.AI); // cả 3 lớp đều ghi AI (DB chỉ nhận MANUAL/AI)
            requirement.setExtractionConfidence(confidence(r.overall()));
            replaceTags(requirement, r.getCodes(), lookup);
            replaceAiOutputs(requirement, r.codes(ExtractionResult.EXPECTED_OUTPUT), lookup);
            CustomerRequirement saved = requirementRepository.save(requirement);

            log.info("D6 requirement {} [{}] lớp={} overall={} quyết định={} notes={} available_input={}",
                    saved.getRequirementCode(), input.mode(), r.getLayer(), r.overall(),
                    ConfidencePolicy.decide(r).decision(), r.getNotes(), r.codes(ExtractionResult.AVAILABLE_INPUT));
            return toResponse(r, input.mode(), lookup, requirementMapper.toRequirementResponse(saved));
        });
    }

    // ------------------------------------------------------------------ Sales xem / sửa mã

    @Override
    @Transactional(readOnly = true)
    public RequirementCodesResponse getCodes(String requirementId) {
        CustomerRequirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUIREMENT_NOT_FOUND));
        return codesResponse(requirement, lookup(), List.of());
    }

    @Override
    @Transactional
    public RequirementCodesResponse updateCodes(String requirementId, RequirementCodesRequest request) {
        CustomerRequirement requirement = loadDraft(requirementId);
        ExtractionResult r = new ExtractionResult();
        r.setCodes(ExtractionResult.OBJECTIVE, distinct(request.getObjectiveCodes()));
        r.setCodes(ExtractionResult.ENVIRONMENT, distinct(request.getEnvironmentCodes()));
        r.setCodes(ExtractionResult.INDUSTRY, distinct(request.getIndustryCodes()));
        r.setCodes(ExtractionResult.TOPIC, distinct(request.getTopicCodes()));
        r.setCodes(ExtractionResult.EXPECTED_OUTPUT, distinct(request.getExpectedOutputCodes()));
        Lookup lookup = lookup();
        List<String> warnings = lookup.dropUnknown(r);
        replaceTags(requirement, r.getCodes(), lookup);
        replaceAiOutputs(requirement, r.codes(ExtractionResult.EXPECTED_OUTPUT), lookup);
        CustomerRequirement saved = requirementRepository.save(requirement);
        log.info("Sales sửa mã requirement {}: {}", saved.getRequirementCode(), r.getCodes());
        return codesResponse(saved, lookup, warnings);
    }

    @Override
    public AiStatusResponse status(boolean ping) {
        return AiStatusResponse.builder()
                .apiKeyConfigured(geminiProperties.isConfigured())
                .models(geminiProperties.getModels())
                .timeoutSeconds(geminiProperties.getTimeoutSeconds())
                .promptVersion(resources.getPromptVersion())
                .promptLength(resources.getSystemPrompt().length())
                .matchingRulesVersion(resources.getMatchingRules().version())
                .ping(ping ? pipeline.ping() : null)
                .build();
    }

    // ------------------------------------------------------------------ helpers

    private CustomerRequirement loadDraft(String requirementId) {
        CustomerRequirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUIREMENT_NOT_FOUND));
        if (requirement.getStatus() == RequirementStatus.CONFIRMED) {
            throw new AppException(ErrorCode.REQUIREMENT_ALREADY_CONFIRMED);
        }
        return requirement;
    }

    /** Các trường form của requirement (đúng tên form_extract.py). Không đưa tên/email/SĐT người liên hệ. */
    private Map<String, Object> formOf(CustomerRequirement requirement) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("objectiveRaw", requirement.getObjectiveRaw());
        String outputs = requirement.getExpectedOutputs().stream()
                .filter(o -> o.getDataItemId() == null)
                .map(RequirementExpectedOutput::getRawExpectedOutput)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
        form.put("expectedOutputs", outputs);
        form.put("providedInputsRaw", requirement.getProvidedInputsRaw());
        form.put("locationDescription", requirement.getLocationDescription());
        form.put("companyName", requirement.getCustomer() == null ? null : requirement.getCustomer().getCompanyName());
        form.put("projectName", requirement.getProjectName());
        return form;
    }

    /** Chế độ câu tự do: chép các trường *_raw Gemini trả về (AI không ghi đè bằng giá trị rỗng). */
    private void applyRawFields(CustomerRequirement requirement, ExtractionResult r) {
        if (StringUtils.hasText(r.getObjectiveRaw())) {
            requirement.setObjectiveRaw(r.getObjectiveRaw());
        }
        if (StringUtils.hasText(r.getIndustryRaw())) {
            requirement.setIndustryRaw(cut(r.getIndustryRaw(), 100));
        }
        if (StringUtils.hasText(r.getEnvironmentRaw())) {
            requirement.setEnvironmentRaw(cut(r.getEnvironmentRaw(), 100));
        }
        if (!StringUtils.hasText(requirement.getProjectName()) && StringUtils.hasText(r.getProjectName())) {
            requirement.setProjectName(r.getProjectName());
        }
        if (r.getAreaValue() != null && r.getAreaUnit() != null) {
            requirement.setAreaValue(BigDecimal.valueOf(r.getAreaValue()));
            requirement.setAreaUnit(r.getAreaUnit());
        }
    }

    private void replaceTags(CustomerRequirement requirement, Map<String, List<String>> codes, Lookup lookup) {
        requirementTagRepository.deleteByRequirementId(requirement.getRequirementId());
        List<RequirementTag> rows = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String field : TAG_FIELDS) {
            for (String code : codes.getOrDefault(field, List.of())) {
                Tag tag = lookup.tags.get(code);
                if (tag != null && seen.add(tag.getTagId())) {
                    rows.add(RequirementTag.builder()
                            .id(new RequirementTagId(requirement.getRequirementId(), tag.getTagId()))
                            .requirement(requirement)
                            .tag(tag)
                            .build());
                }
            }
        }
        requirementTagRepository.saveAll(rows);
    }

    /**
     * Đầu ra kỳ vọng đã chuẩn hóa (có data_item_id) được thay bằng kết quả mới, priority NORMAL.
     * Các dòng khách nhập tay (chỉ có raw_expected_output) được giữ nguyên.
     */
    private void replaceAiOutputs(CustomerRequirement requirement, List<String> outputCodes, Lookup lookup) {
        List<RequirementExpectedOutput> old = requirement.getExpectedOutputs().stream()
                .filter(o -> o.getDataItemId() != null)
                .toList();
        old.forEach(requirement::removeExpectedOutput);
        for (String code : outputCodes) {
            DataItem item = lookup.dataItems.get(code);
            if (item != null) {
                requirement.addExpectedOutput(RequirementExpectedOutput.builder()
                        .dataItemId(item.getDataItemId())
                        .rawExpectedOutput(cut(item.getDataName(), 255))
                        .priority(OutputPriority.NORMAL)
                        .build());
            }
        }
    }

    private RequirementCodesResponse codesResponse(CustomerRequirement requirement, Lookup lookup, List<String> warnings) {
        Map<TagType, List<CodeLabel>> byType = new LinkedHashMap<>();
        requirementTagRepository.findWithTagByRequirementId(requirement.getRequirementId()).forEach(rt ->
                byType.computeIfAbsent(rt.getTag().getTagType(), k -> new ArrayList<>())
                        .add(new CodeLabel(rt.getTag().getTagCode(), rt.getTag().getTagName())));
        Map<String, DataItem> byId = lookup.dataItems.values().stream()
                .collect(Collectors.toMap(DataItem::getDataItemId, Function.identity(), (a, b) -> a));
        List<CodeLabel> outputs = requirement.getExpectedOutputs().stream()
                .map(o -> byId.get(o.getDataItemId()))
                .filter(d -> d != null)
                .map(d -> new CodeLabel(d.getDataCode(), d.getDataName()))
                .distinct()
                .toList();
        return RequirementCodesResponse.builder()
                .requirementId(requirement.getRequirementId())
                .requirementCode(requirement.getRequirementCode())
                .status(requirement.getStatus().name())
                .extractionMethod(requirement.getExtractionMethod() == null ? null : requirement.getExtractionMethod().name())
                .extractionConfidence(requirement.getExtractionConfidence())
                .objectiveCodes(byType.getOrDefault(TagType.OBJECTIVE, List.of()))
                .environmentCodes(byType.getOrDefault(TagType.ENVIRONMENT, List.of()))
                .industryCodes(byType.getOrDefault(TagType.INDUSTRY, List.of()))
                .topicCodes(byType.getOrDefault(TagType.TOPIC, List.of()))
                .expectedOutputCodes(outputs)
                .warnings(warnings)
                .build();
    }

    private ExtractionResponse toResponse(ExtractionResult r, String mode, Lookup lookup,
                                          gascolae.group9.package_builder.customer.dto.response.RequirementResponse saved) {
        ConfidencePolicy.Verdict verdict = ConfidencePolicy.decide(r);
        return ExtractionResponse.builder()
                .mode(mode)
                .layer(r.getLayer())
                .decision(verdict.decision().name())
                .decisionMessage(verdict.reason())
                .recommendationAllowed(verdict.decision() != ConfidencePolicy.Decision.INSUFFICIENT)
                .outOfScope(r.getNotes() != null && r.getNotes().startsWith(OUT_OF_SCOPE_PREFIX))
                .projectName(r.getProjectName())
                .objectiveRaw(r.getObjectiveRaw())
                .industryRaw(r.getIndustryRaw())
                .environmentRaw(r.getEnvironmentRaw())
                .areaValue(r.getAreaValue() == null ? null : BigDecimal.valueOf(r.getAreaValue()))
                .areaUnit(r.getAreaUnit())
                .objectiveCodes(lookup.tagLabels(r.codes(ExtractionResult.OBJECTIVE)))
                .environmentCodes(lookup.tagLabels(r.codes(ExtractionResult.ENVIRONMENT)))
                .industryCodes(lookup.tagLabels(r.codes(ExtractionResult.INDUSTRY)))
                .topicCodes(lookup.tagLabels(r.codes(ExtractionResult.TOPIC)))
                .expectedOutputCodes(lookup.dataLabels(r.codes(ExtractionResult.EXPECTED_OUTPUT)))
                .availableInputCodes(lookup.dataLabels(r.codes(ExtractionResult.AVAILABLE_INPUT)))
                .confidence(r.getConfidence())
                .extractionConfidence(confidence(r.overall()))
                .notes(r.getNotes())
                .warnings(r.getWarnings())
                .previousErrors(r.getPreviousErrors())
                .latencyMs(r.getLatencyMs())
                .promptVersion(resources.getPromptVersion())
                .requirement(saved)
                .build();
    }

    private Lookup lookup() {
        Map<String, Tag> tags = new LinkedHashMap<>();
        tagRepository.findAll().forEach(t -> tags.put(t.getTagCode(), t));
        Map<String, DataItem> items = new LinkedHashMap<>();
        dataItemRepository.findAll().forEach(d -> items.put(d.getDataCode(), d));
        return new Lookup(tags, items);
    }

    private static BigDecimal confidence(double overall) {
        double v = Math.max(0, Math.min(1, overall));
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static List<String> distinct(Collection<String> codes) {
        if (codes == null) {
            return List.of();
        }
        return codes.stream().filter(StringUtils::hasText).map(String::strip).distinct().toList();
    }

    /** Bảng tags + data_items trong DB: bước 2 của hậu kiểm (loại mã không có trong danh sách) và tra tên hiển thị. */
    private record Lookup(Map<String, Tag> tags, Map<String, DataItem> dataItems) {

        /** Bỏ mã không có trong DB hoặc sai loại tag; trả cảnh báo. Không để mã lạ lọt vào DB. */
        List<String> dropUnknown(ExtractionResult r) {
            List<String> warnings = new ArrayList<>();
            for (String field : ExtractionResult.CODE_FIELDS) {
                TagType type = FIELD_TAG_TYPE.get(field);
                List<String> codes = r.codes(field);
                List<String> bad = codes.stream().filter(c -> type != null
                        ? !(tags.containsKey(c) && tags.get(c).getTagType() == type)
                        : !dataItems.containsKey(c)).toList();
                if (!bad.isEmpty()) {
                    warnings.add("Loại mã không có trong DB ở " + field + ": " + PyText.reprList(bad));
                    r.setCodes(field, codes.stream().filter(c -> !bad.contains(c)).toList());
                }
            }
            return warnings;
        }

        List<CodeLabel> tagLabels(List<String> codes) {
            return codes.stream().map(c -> new CodeLabel(c, tags.containsKey(c) ? tags.get(c).getTagName() : c)).toList();
        }

        List<CodeLabel> dataLabels(List<String> codes) {
            return codes.stream().map(c -> new CodeLabel(c, dataItems.containsKey(c) ? dataItems.get(c).getDataName() : c)).toList();
        }
    }
}

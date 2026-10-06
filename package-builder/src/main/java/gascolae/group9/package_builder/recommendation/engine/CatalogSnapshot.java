package gascolae.group9.package_builder.recommendation.engine;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ảnh chụp dữ liệu catalog mà D4 cần: tags (loại, mã cha, tên), tag và đầu ra của từng dịch vụ,
 * tên và "nhóm đầu ra" của data_items. Nạp một lần từ DB rồi chấm điểm thuần trong bộ nhớ.
 */
public class CatalogSnapshot {
    public record TagInfo(String code, String name, String type, String parentCode) {
    }

    public record ServiceInfo(String serviceId, String code, String name, boolean active, String lifecycleStatus) {
    }

    private static final String FAMILY_MARK = "Nhóm đầu ra: ";

    private final Map<String, TagInfo> tags;
    private final Map<String, Set<String>> serviceTags;
    private final Map<String, Set<String>> serviceOutputs;
    private final Map<String, String> dataNames;
    private final Map<String, String> dataFamily;
    private final List<ServiceInfo> services;
    /** Thứ tự thay mã → tên trong câu lý do: mã dài trước (giống _nice() của score.py). */
    private final List<Map.Entry<String, String>> tagNameReplacements;
    private final List<Map.Entry<String, String>> dataNameReplacements;

    public CatalogSnapshot(Collection<TagInfo> tags,
                           Map<String, Set<String>> serviceTags,
                           Map<String, Set<String>> serviceOutputs,
                           Map<String, String> dataNames,
                           Map<String, String> dataDescriptions,
                           List<ServiceInfo> services) {
        this.tags = new LinkedHashMap<>();
        tags.forEach(t -> this.tags.put(t.code(), t));
        this.serviceTags = serviceTags;
        this.serviceOutputs = serviceOutputs;
        this.dataNames = dataNames;
        this.dataFamily = new LinkedHashMap<>();
        dataDescriptions.forEach((code, desc) -> dataFamily.put(code, family(desc)));
        this.services = services;
        this.tagNameReplacements = this.tags.values().stream()
                .map(t -> Map.entry(t.code(), t.name()))
                .sorted(Comparator.comparingInt((Map.Entry<String, String> e) -> -e.getKey().length()))
                .toList();
        this.dataNameReplacements = dataNames.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt((Map.Entry<String, String> e) -> -e.getKey().length()))
                .toList();
    }

    /** "… Nhóm đầu ra: X – …" → "X"; không có thì chuỗi rỗng. */
    static String family(String description) {
        if (description == null) {
            return "";
        }
        int i = description.indexOf(FAMILY_MARK);
        if (i < 0) {
            return "";
        }
        String rest = description.substring(i + FAMILY_MARK.length());
        int j = rest.indexOf(" –");
        return j < 0 ? rest : rest.substring(0, j);
    }

    public Set<String> tagsOf(String serviceCode, String tagType) {
        return serviceTags.getOrDefault(serviceCode, Set.of()).stream()
                .filter(t -> tags.containsKey(t) && tagType.equals(tags.get(t).type()))
                .collect(Collectors.toSet());
    }

    public Set<String> outputsOf(String serviceCode) {
        return serviceOutputs.getOrDefault(serviceCode, Set.of());
    }

    public String parentOf(String tagCode) {
        TagInfo t = tags.get(tagCode);
        return t == null || t.parentCode() == null || t.parentCode().isEmpty() ? null : t.parentCode();
    }

    public String familyOf(String dataCode) {
        return dataFamily.getOrDefault(dataCode, "");
    }

    public List<ServiceInfo> getServices() {
        return services;
    }

    public Map<String, TagInfo> getTags() {
        return tags;
    }

    public Map<String, String> getDataNames() {
        return dataNames;
    }

    /** Đổi mã trong câu giải thích sang nhãn tiếng Việt cho Sales đọc. */
    public String nice(String text) {
        String out = text;
        for (Map.Entry<String, String> e : tagNameReplacements) {
            out = out.replace(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, String> e : dataNameReplacements) {
            out = out.replace(e.getKey(), e.getValue());
        }
        return out;
    }
}

package gascolae.group9.package_builder.recommendation.service;

import gascolae.group9.package_builder.catalog.entity.DataItem;
import gascolae.group9.package_builder.catalog.repository.DataItemRepository;
import gascolae.group9.package_builder.catalog.repository.ServiceOutputRepository;
import gascolae.group9.package_builder.catalog.repository.ServiceRepository;
import gascolae.group9.package_builder.catalog.repository.ServiceTagRepository;
import gascolae.group9.package_builder.catalog.repository.TagRepository;
import gascolae.group9.package_builder.recommendation.engine.CatalogSnapshot;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Chụp dữ liệu catalog từ DB (tags, service_tags, service_outputs, data_items, services) cho engine D4. */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CatalogSnapshotLoader {
    TagRepository tagRepository;
    ServiceTagRepository serviceTagRepository;
    ServiceOutputRepository serviceOutputRepository;
    DataItemRepository dataItemRepository;
    ServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public CatalogSnapshot load() {
        List<CatalogSnapshot.TagInfo> tags = tagRepository.findAll().stream()
                .map(t -> new CatalogSnapshot.TagInfo(t.getTagCode(), t.getTagName(), t.getTagType().name(),
                        t.getParentTag() == null ? null : t.getParentTag().getTagCode()))
                .toList();

        Map<String, Set<String>> serviceTags = new LinkedHashMap<>();
        serviceTagRepository.findAll().forEach(st -> serviceTags
                .computeIfAbsent(st.getService().getServiceCode(), k -> new LinkedHashSet<>())
                .add(st.getTag().getTagCode()));

        Map<String, Set<String>> serviceOutputs = new LinkedHashMap<>();
        serviceOutputRepository.findAll().forEach(so -> serviceOutputs
                .computeIfAbsent(so.getService().getServiceCode(), k -> new LinkedHashSet<>())
                .add(so.getDataItem().getDataCode()));

        Map<String, String> names = new LinkedHashMap<>();
        Map<String, String> descriptions = new LinkedHashMap<>();
        for (DataItem d : dataItemRepository.findAll()) {
            names.put(d.getDataCode(), d.getDataName());
            descriptions.put(d.getDataCode(), d.getDescription());
        }

        List<CatalogSnapshot.ServiceInfo> services = serviceRepository.findAll().stream()
                .map(s -> new CatalogSnapshot.ServiceInfo(s.getServiceId(), s.getServiceCode(), s.getServiceName(),
                        Boolean.TRUE.equals(s.getActive()),
                        s.getLifecycleStatus() == null ? null : s.getLifecycleStatus().name()))
                .toList();

        return new CatalogSnapshot(tags, serviceTags, serviceOutputs, names, descriptions, services);
    }
}

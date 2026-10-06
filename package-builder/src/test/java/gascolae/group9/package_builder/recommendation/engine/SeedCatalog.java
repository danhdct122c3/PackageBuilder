package gascolae.group9.package_builder.recommendation.engine;

import gascolae.group9.package_builder.catalog.util.SimpleCsvParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dựng CatalogSnapshot thẳng từ seed CSV (cùng dữ liệu CatalogDataInitializer nạp vào DB) để test không cần DB. */
final class SeedCatalog {
    private SeedCatalog() {
    }

    static List<Map<String, String>> csv(String name) throws Exception {
        try (InputStream is = SeedCatalog.class.getResourceAsStream("/seed/" + name + ".csv")) {
            return SimpleCsvParser.parse(is);
        }
    }

    static CatalogSnapshot load() throws Exception {
        List<CatalogSnapshot.TagInfo> tags = new ArrayList<>();
        for (Map<String, String> t : csv("tags")) {
            tags.add(new CatalogSnapshot.TagInfo(t.get("tag_code"), t.get("tag_name"), t.get("tag_type"), t.get("parent_tag_code")));
        }
        Map<String, Set<String>> svcTags = new LinkedHashMap<>();
        for (Map<String, String> r : csv("service_tags")) {
            svcTags.computeIfAbsent(r.get("service_code"), k -> new LinkedHashSet<>()).add(r.get("tag_code"));
        }
        Map<String, Set<String>> svcOut = new LinkedHashMap<>();
        for (Map<String, String> r : csv("service_outputs")) {
            svcOut.computeIfAbsent(r.get("service_code"), k -> new LinkedHashSet<>()).add(r.get("data_code"));
        }
        Map<String, String> names = new LinkedHashMap<>();
        Map<String, String> desc = new LinkedHashMap<>();
        for (Map<String, String> d : csv("data_items")) {
            names.put(d.get("data_code"), d.get("data_name"));
            desc.put(d.get("data_code"), d.get("description"));
        }
        List<CatalogSnapshot.ServiceInfo> services = new ArrayList<>();
        for (Map<String, String> s : csv("services")) {
            services.add(new CatalogSnapshot.ServiceInfo(s.get("service_code"), s.get("service_code"), s.get("service_name"),
                    "TRUE".equalsIgnoreCase(s.get("active")), s.get("lifecycle_status")));
        }
        return new CatalogSnapshot(tags, svcTags, svcOut, names, desc, services);
    }
}

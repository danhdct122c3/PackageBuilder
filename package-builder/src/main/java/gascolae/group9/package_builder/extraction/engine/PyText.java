package gascolae.group9.package_builder.extraction.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Các hàm nhỏ để bản Java cho kết quả giống hệt bản tham chiếu Python của SV3
 * (cách bỏ dấu, cách làm tròn, cách in danh sách trong cảnh báo).
 */
public final class PyText {
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{Mn}+");

    private PyText() {
    }

    /** Tương đương {@code norm()} trong fallback_extract.py: chữ thường, tách dấu NFD rồi bỏ dấu. */
    public static String stripAccents(String s) {
        if (s == null) {
            return "";
        }
        String nfd = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return COMBINING_MARKS.matcher(nfd).replaceAll("");
    }

    /** Tương đương {@code round(x, 2)} của Python (làm tròn đúng giá trị nhị phân, half-even). */
    public static double round2(double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }

    /** In danh sách giống {@code repr(list)} của Python: ['A', 'B']. */
    public static String reprList(Collection<?> items) {
        return items.stream()
                .map(i -> i == null ? "None" : "'" + i.toString().replace("\\", "\\\\").replace("'", "\\'") + "'")
                .collect(Collectors.joining(", ", "[", "]"));
    }

    /** In số thực giống {@code str(float)} của Python cho khoảng giá trị điểm 0–100. */
    public static String pyFloat(double value) {
        return Double.toString(value);
    }
}

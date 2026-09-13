package com.example.jobfinder.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Chuẩn hoá chuỗi tiếng Việt để so khớp tìm kiếm chính xác, không phụ thuộc
 * hoa/thường hay dấu câu, khoảng trắng thừa:
 *   "Hà Nội"  ->  "ha noi"
 *   "  Đà  Nẵng "  ->  "da nang"
 *
 * Việc chuẩn hoá cả dữ liệu lưu trữ lẫn từ khoá người dùng nhập giúp so khớp
 * đúng bản chất nội dung thay vì so khớp chuỗi thô (dễ sai vì dấu tiếng Việt),
 * đây là nền tảng để bộ lọc tìm kiếm hoạt động "chặt chẽ": mọi điều kiện đều
 * được so khớp trên cùng 1 chuẩn dữ liệu, không có trường hợp lọt vì khác dấu.
 */
public final class SearchUtils {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private SearchUtils() { }

    public static String normalize(String input) {
        if (input == null) return "";
        String result = input.trim().toLowerCase();
        result = result.replace('đ', 'd').replace('Đ', 'd');
        result = Normalizer.normalize(result, Normalizer.Form.NFD);
        result = DIACRITICS.matcher(result).replaceAll("");
        result = MULTI_SPACE.matcher(result).replaceAll(" ");
        return result.trim();
    }

    public static String[] toWords(String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isEmpty()) return new String[0];
        return normalizedQuery.split(" ");
    }

    /** True nếu "haystack" (đã chuẩn hoá hoặc chưa đều được) chứa TẤT CẢ các từ trong "words". */
    public static boolean containsAllWords(String haystackRaw, String[] words) {
        if (words.length == 0) return true;
        String haystack = normalize(haystackRaw);
        for (String w : words) {
            if (!haystack.contains(w)) return false;
        }
        return true;
    }
}

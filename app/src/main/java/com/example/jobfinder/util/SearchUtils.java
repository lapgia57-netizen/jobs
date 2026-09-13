package com.example.jobfinder.util;

import com.example.jobfinder.model.Job;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Chuẩn hoá tiếng Việt + lọc AND chặt chẽ trên title/company/location + lương tối thiểu.
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

    public static boolean containsAllWords(String haystackRaw, String[] words) {
        if (words.length == 0) return true;
        String haystack = normalize(haystackRaw);
        for (String w : words) {
            if (!haystack.contains(w)) return false;
        }
        return true;
    }

    /**
     * Mọi điều kiện đã nhập phải đúng đồng thời (AND).
     * keyword: tất cả từ phải xuất hiện trong title + company + location.
     * location: tất cả từ phải xuất hiện trong location.
     * minSalary (triệu): salaryMax >= minSalary * 1_000_000 (nếu salaryMax > 0),
     *   hoặc salaryMin >= minSalary * 1_000_000.
     */
    public static boolean matches(Job job, String keyword, String location, long minSalaryMillion) {
        if (job == null) return false;

        String kw = normalize(keyword);
        if (!kw.isEmpty()) {
            String hay = (nullToEmpty(job.getTitle()) + " "
                    + nullToEmpty(job.getCompany()) + " "
                    + nullToEmpty(job.getLocation()));
            if (!containsAllWords(hay, toWords(kw))) return false;
        }

        String loc = normalize(location);
        if (!loc.isEmpty()) {
            if (!containsAllWords(nullToEmpty(job.getLocation()), toWords(loc))) return false;
        }

        if (minSalaryMillion > 0) {
            long threshold = minSalaryMillion * 1_000_000L;
            long max = job.getSalaryMax();
            long min = job.getSalaryMin();
            // Ưu tiên salaryMax; nếu không có thì dùng salaryMin
            if (max > 0) {
                if (max < threshold) return false;
            } else if (min > 0) {
                if (min < threshold) return false;
            } else {
                // Không có số lương → không khớp bộ lọc lương
                return false;
            }
        }
        return true;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

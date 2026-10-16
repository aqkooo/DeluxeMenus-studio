package com.ejyqyl.dmstudio.service.diff;

import java.util.*;

/**
 * Clean line-by-line diff engine for DeluxeMenus configurations (§Diff Viewer).
 */
public class DiffEngine {

    public enum DiffType {
        UNCHANGED,
        ADDED,
        REMOVED
    }

    public record DiffLine(
            DiffType type,
            Integer oldLineNo,
            Integer newLineNo,
            String text
    ) {}

    public record DiffReport(
            List<DiffLine> lines,
            int addedCount,
            int removedCount,
            int unchangedCount
    ) {
        public boolean hasChanges() {
            return addedCount > 0 || removedCount > 0;
        }
    }

    public static DiffReport compare(String originalText, String modifiedText) {
        List<String> orig = originalText != null ? Arrays.asList(originalText.split("\r?\n", -1)) : List.of();
        List<String> mod = modifiedText != null ? Arrays.asList(modifiedText.split("\r?\n", -1)) : List.of();

        // Strip trailing empty lines if both end with newline
        if (!orig.isEmpty() && orig.get(orig.size() - 1).isEmpty()) orig = orig.subList(0, orig.size() - 1);
        if (!mod.isEmpty() && mod.get(mod.size() - 1).isEmpty()) mod = mod.subList(0, mod.size() - 1);

        int[][] lcs = computeLcsMatrix(orig, mod);
        List<DiffLine> result = new ArrayList<>();

        int i = orig.size();
        int j = mod.size();
        int added = 0;
        int removed = 0;
        int unchanged = 0;

        List<DiffLine> backtrack = new ArrayList<>();
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && Objects.equals(orig.get(i - 1), mod.get(j - 1))) {
                backtrack.add(new DiffLine(DiffType.UNCHANGED, i, j, orig.get(i - 1)));
                i--;
                j--;
                unchanged++;
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                backtrack.add(new DiffLine(DiffType.ADDED, null, j, mod.get(j - 1)));
                j--;
                added++;
            } else if (i > 0 && (j == 0 || lcs[i][j - 1] < lcs[i - 1][j])) {
                backtrack.add(new DiffLine(DiffType.REMOVED, i, null, orig.get(i - 1)));
                i--;
                removed++;
            }
        }

        Collections.reverse(backtrack);
        return new DiffReport(backtrack, added, removed, unchanged);
    }

    private static int[][] computeLcsMatrix(List<String> a, List<String> b) {
        int m = a.size();
        int n = b.size();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (Objects.equals(a.get(i - 1), b.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp;
    }
}

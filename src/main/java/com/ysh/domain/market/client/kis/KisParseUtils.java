package com.ysh.domain.market.client.kis;

/**
 * KIS 응답 매핑 시 공통 변환 로직
 *
 */
public final class KisParseUtils {
    private KisParseUtils() {
    }

    public static double parseDoubleSafe(String s) {
        if (s == null || s.isBlank()) return 0.0;
        try {
            // KIS 해외 rate는 "+1.17" 부호 prefix 포함 — Double.parseDouble은 그대로 처리
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static long parseLongSafe(String s) {
        if (s == null || s.isBlank()) return 0L;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * KIS sign 코드에 따라 절대값에 부호 적용.
     */
    public static double applySign(double value, String sign) {
        double abs = Math.abs(value);
        if ("4".equals(sign) || "5".equals(sign)) {
            return -abs;
        }
        return abs;
    }
}

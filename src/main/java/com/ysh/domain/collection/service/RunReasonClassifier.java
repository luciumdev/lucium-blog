package com.ysh.domain.collection.service;

import com.ysh.domain.collection.entity.RunStatus;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 실행 기록 message → 공개용 사유.
 *
 * <ul>
 *   <li>작업 코드가 직접 쓴 사유("응답 없음: KOSPI" 등)는 그대로 노출 (URL 만 가림)</li>
 *   <li>예외 원문("XxxException: ...")은 URL·키·호스트가 섞일 수 있어 분류 문구로만 노출</li>
 * </ul>
 * 원문은 관리자 로그인 상태에서만 rawMessage 로 별도 제공.
 */
public final class RunReasonClassifier {

    /** CollectionRunRecorder 가 남기는 형식: "SimpleName: message" */
    private static final Pattern EXCEPTION_PREFIX = Pattern.compile("^[A-Za-z0-9_$.]+(Exception|Error)\\b");
    private static final Pattern URL = Pattern.compile("https?://\\S+");
    private static final Pattern AUTH = Pattern.compile("\\b(401|403)\\b|unauthorized|forbidden");
    private static final Pattern RATE_LIMIT = Pattern.compile("\\b429\\b|too many requests");
    private static final Pattern SERVER_ERROR = Pattern.compile("\\b5\\d{2}\\b|server error|service unavailable|bad gateway");

    private RunReasonClassifier() {
    }

    public static String toPublic(RunStatus status, String message) {
        if (message == null || message.isBlank()) {
            return status == RunStatus.SUCCESS ? null : defaultReason(status);
        }
        if (!EXCEPTION_PREFIX.matcher(message).find()) {
            return URL.matcher(message).replaceAll("[URL]");
        }

        String m = message.toLowerCase(Locale.ROOT);
        if (m.contains("timeout") || m.contains("timed out")) return "외부 API 응답 시간 초과";
        if (AUTH.matcher(m).find()) return "외부 API 인증 실패";
        if (RATE_LIMIT.matcher(m).find()) return "외부 API 호출 한도 초과";
        if (SERVER_ERROR.matcher(m).find()) return "외부 API 서버 오류";
        if (m.contains("connect") || m.contains("unknownhost") || m.contains("i/o error")) return "외부 API 연결 실패";
        if (m.contains("json") || m.contains("pars") || m.contains("deserializ") || m.contains("mismatch")) {
            return "응답 형식 오류";
        }
        return "내부 처리 오류";
    }

    private static String defaultReason(RunStatus status) {
        return switch (status) {
            case PARTIAL -> "일부 실패";
            case EMPTY -> "데이터 없음";
            case FAILED -> "실패";
            case SUCCESS -> null;
        };
    }
}

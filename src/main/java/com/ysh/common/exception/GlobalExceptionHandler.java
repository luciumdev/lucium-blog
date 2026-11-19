package com.ysh.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 전역 예외 핸들러 — RFC 9457 ProblemDetail 형식으로 응답
 *
 * application.yml에 spring.mvc.problemdetails.enabled=true 설정 필수
 *
 * 응답 예시:
 * {
 *   "type":     "https://api.lucium.dev/errors/post-not-found",
 *   "title":    "Not Found",
 *   "status":   404,
 *   "detail":   "게시글을 찾을 수 없습니다.",
 *   "instance": "/v1/posts/550e8400-..."
 * }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String BASE_URI = "https://api.lucium.dev/errors/";

    // 비즈니스 예외 — ErrorCode 기반
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException e,
                                                 HttpServletRequest request) {
        log.warn("BusinessException: code={}, message={}", e.getErrorCode(), e.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                e.getErrorCode().getHttpStatus(),
                e.getMessage()
        );
        problem.setType(URI.create(BASE_URI + toKebabCase(e.getErrorCode().name())));
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    // @Valid 검증 실패 — 필드별 에러 목록 포함
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e,
                                          HttpServletRequest request) {
        List<Map<String, String>> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorMap)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(BASE_URI + "validation-failed"));
        problem.setTitle("Validation Failed");
        problem.setDetail("입력값이 올바르지 않습니다.");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errors", errors);
        return problem;
    }

    // 예상치 못한 서버 오류
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception e,
                                         HttpServletRequest request) {
        // 스택트레이스는 서버 로그에만 남김 — 클라이언트 응답에 포함 금지
        log.error("Unhandled exception: uri={}", request.getRequestURI(), e);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(URI.create(BASE_URI + "internal-server-error"));
        problem.setDetail("서버 내부 오류가 발생했습니다.");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
// GlobalExceptionHandler에 추가

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParam(
            MissingServletRequestParameterException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("필수 파라미터 누락");
        problem.setDetail("필수 요청 파라미터가 없습니다: " + e.getParameterName());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("잘못된 파라미터 형식");
        problem.setDetail("파라미터 '" + e.getName() + "' 값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(problem);
    }
    // ------------------------------------
    // 유틸
    // ------------------------------------
    private Map<String, String> toFieldErrorMap(FieldError fe) {
        return Map.of(
                "field",   fe.getField(),
                "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "유효하지 않은 값"
        );
    }

    /** POST_NOT_FOUND → post-not-found */
    private String toKebabCase(String name) {
        return name.toLowerCase().replace('_', '-');
    }

}

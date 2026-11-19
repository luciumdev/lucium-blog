package com.ysh.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@RequiredArgsConstructor
@Getter
public enum ErrorCode {
    INVALID_CREDENTIALS      (HttpStatus.UNAUTHORIZED,  "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN            (HttpStatus.UNAUTHORIZED,  "유효하지 않은 토큰입니다."),
    FORBIDDEN                (HttpStatus.FORBIDDEN,     "접근 권한이 없습니다."),

    POST_NOT_FOUND           (HttpStatus.NOT_FOUND,     "게시글을 찾을 수 없습니다."),
    POST_NOT_PUBLISHED       (HttpStatus.NOT_FOUND,     "게시글을 찾을 수 없습니다."),

    COMMENT_NOT_FOUND        (HttpStatus.NOT_FOUND,     "댓글을 찾을 수 없습니다."),
    COMMENT_PASSWORD_INVALID (HttpStatus.UNAUTHORIZED,  "댓글 비밀번호가 올바르지 않습니다."),

    CATEGORY_NOT_FOUND       (HttpStatus.NOT_FOUND,     "카테고리를 찾을 수 없습니다."),
    TAG_NOT_FOUND            (HttpStatus.NOT_FOUND,     "태그를 찾을 수 없습니다."),
    DUPLICATE_TAG_NAME       (HttpStatus.CONFLICT,      "이미 존재하는 태그입니다."),

    FILE_UPLOAD_FAILED       (HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_DELETE_FAILED       (HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다."),
    INVALID_FILE_TYPE        (HttpStatus.BAD_REQUEST,   "허용되지 않는 파일 형식입니다."),

    MARKET_DATA_UNAVAILABLE  (HttpStatus.SERVICE_UNAVAILABLE, "마켓 데이터를 불러올 수 없습니다."),

    INVALID_INPUT            (HttpStatus.BAD_REQUEST,   "입력값이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String     message;


}


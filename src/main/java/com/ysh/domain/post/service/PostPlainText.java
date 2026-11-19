package com.ysh.domain.post.service;

import com.ysh.domain.post.entity.ContentFormat;

import java.util.regex.Pattern;

/**
 * 검색 색인용 본문 텍스트.
 * HTML 글은 태그·스크립트·스타일이 그대로 색인되면 검색 노이즈(div, class 등)가 되므로 텍스트만 남긴다.
 * 마크다운은 기존과 동일하게 원문 그대로 색인.
 *
 * 외부 파서(jsoup) 없이 정규식으로 처리 — 색인용이라 완벽한 파싱은 필요 없음.
 */
public final class PostPlainText {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
    private static final Pattern NON_TEXT_BLOCK = Pattern.compile(
            "<(script|style|noscript|template|svg|head)\\b[^>]*>.*?</\\1\\s*>", FLAGS);
    private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private PostPlainText() {
    }

    public static String of(String content, ContentFormat format) {
        if (content == null) return "";
        if (format != ContentFormat.HTML) return content;

        String text = NON_TEXT_BLOCK.matcher(content).replaceAll(" ");
        text = COMMENT.matcher(text).replaceAll(" ");
        text = TAG.matcher(text).replaceAll(" ");
        text = decodeBasicEntities(text);
        return WHITESPACE.matcher(text).replaceAll(" ").trim();
    }

    private static String decodeBasicEntities(String s) {
        // &amp; 는 마지막에 — 먼저 바꾸면 "&amp;lt;" 가 "<" 로 이중 디코딩됨
        return s.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
    }
}

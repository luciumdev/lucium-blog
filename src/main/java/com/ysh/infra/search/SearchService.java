package com.ysh.infra.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_TAGS = "tags";
    /** 태그가 정확히 일치하면 본문 일치보다 위로 */
    private static final float TAG_BOOST = 3.0f;
    private static final String HIGHLIGHT_PRE = "<em>";
    private static final String HIGHLIGHT_POST = "</em>";
    /**
     * 프론트가 title/snippet을 dangerouslySetInnerHTML로 렌더하므로
     * 원문 HTML은 반드시 이스케이프 — <em> 태그만 살아남게 한다. (XSS 차단)
     */
    private static final String HIGHLIGHT_ENCODER_HTML = "html";

    private final ElasticsearchOperations operations;

    public Page<SearchResponse> search(String keyword, Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            return Page.empty(pageable);
        }

        NativeQueryBuilder builder = NativeQuery.builder();
        builder.withQuery(buildQuery(keyword.strip()));
        builder.withHighlightQuery(buildHighlight());
        builder.withPageable(pageable);
        NativeQuery query = builder.build();

        SearchHits<PostSearchDocument> hits = operations.search(query, PostSearchDocument.class);

        List<SearchResponse> content = hits.stream()
                .map(this::toResponse)
                .toList();
        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }


    // -------------------private-------------------------------------
    /**
     * 본문 검색(nori) OR 태그 일치.
     * <p>tags 는 keyword 타입이라 multi_match 에 넣으면 형태소 분석 없이 전체 문자열 일치만 됨 →
     * 별도 term 쿼리(대소문자 무시)로 처리. "#Redis" 처럼 앞에 # 를 붙여도 찾도록 제거.
     * 매핑 변경이 아니라 쿼리만 바꾼 것이라 재색인 불필요.
     */
    private Query buildQuery(String keyword) {
        String tag = keyword.replaceFirst("^#+", "").strip();

        Query text = Query.of(q -> q
                .multiMatch(m -> m
                        .query(keyword)
                        .fields(FIELD_TITLE + "^2", FIELD_CONTENT, FIELD_SUMMARY)
                        .type(TextQueryType.BestFields)
                )
        );
        if (tag.isEmpty()) {
            return text;
        }
        Query tagMatch = Query.of(q -> q
                .term(t -> t
                        .field(FIELD_TAGS)
                        .value(tag)
                        .caseInsensitive(true)
                        .boost(TAG_BOOST)
                )
        );
        return Query.of(q -> q
                .bool(b -> b
                        .should(text)
                        .should(tagMatch)
                        .minimumShouldMatch("1")
                )
        );
    }

    private HighlightQuery buildHighlight() {
        HighlightFieldParameters fragmentParams = HighlightFieldParameters.builder()
                .withFragmentSize(150)
                .withNumberOfFragments(1)
                .withPreTags(HIGHLIGHT_PRE)
                .withPostTags(HIGHLIGHT_POST)
                .build();
        Highlight highlight = new Highlight(
                HighlightParameters.builder()
                        .withRequireFieldMatch(false)
                        .withEncoder(HIGHLIGHT_ENCODER_HTML)
                        .build(),
                List.of(
                        new HighlightField(FIELD_TITLE, fragmentParams),
                        new HighlightField(FIELD_CONTENT, fragmentParams),
                        new HighlightField(FIELD_SUMMARY, fragmentParams)
                ));
        return new HighlightQuery(highlight, PostSearchDocument.class);
    }

    private SearchResponse toResponse(SearchHit<PostSearchDocument> hit) {
        PostSearchDocument doc = hit.getContent();

        // 하이라이트가 없을 때의 원문 fallback도 동일하게 이스케이프
        String snippet = firstHighlight(hit, FIELD_CONTENT)
                .or(() -> firstHighlight(hit, FIELD_SUMMARY))
                .orElseGet(() -> escape(doc.getSummary()));
        String highlightTitle = firstHighlight(hit, FIELD_TITLE)
                .orElseGet(() -> escape(doc.getTitle()));

        return SearchResponse.builder()
                .publicId(UUID.fromString(doc.getPublicId()))
                .title(highlightTitle)
                .snippet(snippet)
                .categoryName(doc.getCategoryName())
                .tags(doc.getTags())
                .publishedAt(doc.getPublishedAt())
                .score(hit.getScore())
                .build();
    }

    private Optional<String> firstHighlight(SearchHit<PostSearchDocument> hit, String field) {
        List<String> fragments = hit.getHighlightField(field);
        return (fragments == null || fragments.isEmpty())
                ? Optional.empty()
                : Optional.of(fragments.get(0));
    }

    private static String escape(String raw) {
        return raw == null ? null : HtmlUtils.htmlEscape(raw);
    }
}
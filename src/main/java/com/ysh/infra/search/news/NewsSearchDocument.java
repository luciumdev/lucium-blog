package com.ysh.infra.search.news;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ES 뉴스 인덱스 문서 — 집계(공통 키워드, 일별 추이) 전용 read model.
 * 원본은 PostgreSQL news_feeds. 보강(enrichment) 완료 건만 색인.
 *
 * <ul>
 *   <li>keywords / category / source → keyword 타입 (terms 집계·필터)</li>
 *   <li>title / titleKo → nori text (추후 뉴스 검색 확장용)</li>
 *   <li>publishedAt → KST 로컬 시각을 그대로 저장. date_histogram 도 time_zone 없이 돌려
 *       버킷 날짜가 KST 날짜와 일치하게 한다.</li>
 * </ul>
 * 분석기 설정은 posts 인덱스와 동일한 nori 설정 재사용.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = "news")
@Setting(settingPath = "elasticsearch/post-setting.json")
public class NewsSearchDocument {

    static final String FIELD_CATEGORY = "category";
    static final String FIELD_KEYWORDS = "keywords";
    static final String FIELD_PUBLISHED_AT = "publishedAt";

    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String source;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String titleKo;

    @Field(type = FieldType.Keyword)
    private List<String> keywords;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime publishedAt;

    public NewsSearchDocument(Long id, String category, String source, String title,
                              String titleKo, List<String> keywords, LocalDateTime publishedAt) {
        this.id = String.valueOf(id);
        this.category = category;
        this.source = source;
        this.title = title;
        this.titleKo = titleKo;
        this.keywords = keywords;
        this.publishedAt = publishedAt;
    }
}

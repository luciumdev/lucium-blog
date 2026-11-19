package com.ysh.infra.search;

import com.ysh.domain.post.entity.Post;
import com.ysh.domain.post.service.PostPlainText;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ES 인덱스 매칭 (api 응답 모델 아님)
 * 인덱스명: posts
 * 분석기: nori_analyzer
 *
 * 1. ID = publicId (UUID).
 * 2. 인덱스명 단수형 'posts' 고정.
 * 3. title/content/summary만 nori, 나머지는 keyword.
 *    categoryName/tags는 정확 일치 필터링용 → keyword.
 * 4. nori_tokenizer + nori_part_of_speech 필터 조합.
 *    조사·어미(JKB, JKO, EP 등)는 검색 노이즈라 제거.
 * 5. settings JSON은 /resources/elasticsearch/post-setting.json 외부화.
 * 6. HTML 글은 태그를 걷어낸 텍스트만 content 에 색인 (PostPlainText).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = "posts")
@Setting(settingPath = "elasticsearch/post-setting.json")
public class PostSearchDocument {
    @Id
    @Field(type= FieldType.Keyword)
    private String publicId;

    @Field(type= FieldType.Text, analyzer = "nori_analyzer")
    private String title;

    @Field(type= FieldType.Text, analyzer = "nori_analyzer")
    private String content;

    @Field(type= FieldType.Text, analyzer = "nori_analyzer")
    private String summary;

    @Field(type= FieldType.Keyword)
    private String categoryName;

    @Field(type= FieldType.Keyword)
    private List<String> tags;

    @Field(type= FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime publishedAt;

    @Builder(access = AccessLevel.PRIVATE)
    public PostSearchDocument(String publicId, String title, String content, String summary, String categoryName, List<String> tags, LocalDateTime publishedAt) {
        this.publicId = publicId;
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.categoryName = categoryName;
        this.tags = tags;
        this.publishedAt = publishedAt;
    }

    public static PostSearchDocument from(Post post) {
        return PostSearchDocument.builder()
                .publicId(post.getPublicId().toString())
                .title(post.getTitle())
                .content(PostPlainText.of(post.getContent(), post.getContentFormat()))
                .summary(post.getSummary())
                .categoryName(post.getCategory() !=null ? post.getCategory().getName() : null)
                .tags(post.getPostTags().stream().map(pt -> pt.getTag().getName()).toList())
                .publishedAt(post.getPublishedAt())
                .build();
    }
}

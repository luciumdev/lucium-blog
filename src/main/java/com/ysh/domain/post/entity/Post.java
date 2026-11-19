package com.ysh.domain.post.entity;

import com.ysh.domain.category.entity.Category;
import com.ysh.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "posts")
@NoArgsConstructor
@Getter
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;                            // 내부 PK — 절대 API 응답에 포함 금지

    @Column(name = "public_id", nullable = false, updatable = false)
    @UuidGenerator                              // Hibernate 6 UUID 자동 생성
    private UUID publicId;                      // 외부 노출 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 본문 형식 — MARKDOWN(기본) / HTML(원본 파일 게시). V11 */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_format", nullable = false, length = 10)
    private ContentFormat contentFormat;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 500)
    private String thumbnail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @Column(nullable = false)
    private long viewCount;

    private LocalDateTime publishedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTag> postTags = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = PostStatus.DRAFT;
        if (contentFormat == null) contentFormat = ContentFormat.MARKDOWN;
        if (viewCount == 0) viewCount = 0L;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    private Post(User author, Category category, String title, String content,
                 ContentFormat contentFormat, String summary, String thumbnail) {
        this.author = author;
        this.category = category;
        this.title = title;
        this.content = content;
        this.contentFormat = contentFormat != null ? contentFormat : ContentFormat.MARKDOWN;
        this.summary = summary;
        this.thumbnail = thumbnail;
    }

    // ── 도메인 메서드 ──────────────────────────────────────────

    /**
     * @param contentFormat null 이면 기존 형식 유지 (구버전 클라이언트 호환)
     */
    public void update(String title, String content, ContentFormat contentFormat,
                       String summary, String thumbnail, Category category) {
        this.title = title;
        this.content = content;
        if (contentFormat != null) this.contentFormat = contentFormat;
        this.summary = summary;
        this.thumbnail = thumbnail;
        this.category = category;
    }

    public void publish() {
        if (this.status == PostStatus.PUBLISHED) return;
        this.status = PostStatus.PUBLISHED;
        if (this.publishedAt == null) this.publishedAt = LocalDateTime.now();
    }

    public void unpublish() {
        this.status = PostStatus.DRAFT;
    }

    public boolean isPublished() {
        return this.status == PostStatus.PUBLISHED;
    }
}

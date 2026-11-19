package com.ysh.domain.comment.entity;

import com.ysh.domain.post.entity.Post;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    @UuidGenerator
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(nullable = false, length = 50)
    private String authorName;

    @Column(nullable = false, length = 255)
    private String password;            // BCrypt

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean isDeleted;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isDeleted = false;
    }

    @Builder
    private Comment(Post post, Comment parent, String authorName,
                    String password, String content) {
        this.post       = post;
        this.parent     = parent;
        this.authorName = authorName;
        this.password   = password;
        this.content    = content;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.content   = "삭제된 댓글입니다.";
    }



}

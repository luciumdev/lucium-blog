package com.ysh.domain.tag.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(nullable = false)
    private int postCount;

    @PrePersist
    protected void onCreate() {
        if (slug == null && name != null) {
            slug = name.toLowerCase().replaceAll("[^a-z0-9가-힣]", "-");
        }
    }

    @Builder
    private Tag(String name) {
        this.name      = name;
        this.postCount = 0;
    }

    public void incrementPostCount() { this.postCount++; }
    public void decrementPostCount() { if (this.postCount > 0) this.postCount--; }
}

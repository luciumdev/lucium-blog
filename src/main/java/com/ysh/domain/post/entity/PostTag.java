package com.ysh.domain.post.entity;


import com.ysh.domain.tag.entity.Tag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_tags")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PostTag {
    @EmbeddedId
    private PostTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("postId")
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id")
    private Tag tag;

    public PostTag(Post post, Tag tag) {
        this.post = post;
        this.tag  = tag;
        this.id   = new PostTagId(post.getId(), tag.getId());
    }
}

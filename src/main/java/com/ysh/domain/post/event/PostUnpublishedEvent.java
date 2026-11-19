package com.ysh.domain.post.event;

import com.ysh.domain.post.entity.Post;

import java.util.UUID;
/**
 * Post DRAFT로 전환되거나 영구 삭제될 때 발행.
 * 발행 위치: PostService.togglePublish(false), PostService.deletePost()
 *
 */
public record PostUnpublishedEvent(Long postId, UUID publicId) {
    public static PostUnpublishedEvent of(Post post) {
        if (post.getId() == null){
            throw new IllegalArgumentException("PostPublishedEvent는 save() 이후에 생성해야 합니다.");
        }
        return new PostUnpublishedEvent(post.getId(), post.getPublicId());
    }
}

package com.ysh.domain.post.event;

import com.ysh.domain.post.entity.Post;

import java.util.UUID;

/**
 * Post가 PUBLISHED 상태로 진입하거나 publish 상태에서 업데이트될 때 발행
 * 발행 위치: PostService.togglePublish(false), PostService.deletePost()
 * 발행 위치:
 *   {@code PostService.createPost} — 신규 작성 + status=PUBLISHED
 *   {@code PostService.updatePost} — 업데이트 후 publish 상태 유지 (재인덱싱)
 *   {@code PostService.togglePublish(true)} — DRAFT → PUBLISHED 전환
 *
 * 리스너 {@code PostSearchIndexer.onPostPublished} — ES 인덱스 save (upsert).
 */
public record PostPublishedEvent(Long postId, UUID publicId) {
    public static PostPublishedEvent of(Post post) {
        if (post.getId() == null) {
            throw new IllegalArgumentException("PostPublishedEvent는 save() 이후에 생성해야 합니다.");
        }
        return new PostPublishedEvent(post.getId(), post.getPublicId());
    }
}
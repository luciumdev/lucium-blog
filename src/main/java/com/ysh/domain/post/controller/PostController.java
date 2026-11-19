package com.ysh.domain.post.controller;

import com.ysh.common.annotation.VisitorId;
import com.ysh.common.response.ApiResponse;
import com.ysh.domain.post.dto.PostCreateRequest;
import com.ysh.domain.post.dto.PostDetailResponse;
import com.ysh.domain.post.dto.PostSummaryResponse;
import com.ysh.domain.post.dto.PostUpdateRequest;
import com.ysh.domain.post.entity.PostStatus;
import com.ysh.domain.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    //////////// 공통 ////////////
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> getPost (@RequestParam(required = false) String category,
                                                                           @RequestParam(required = false) String tag,
                                                                           @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getPublishedPosts(category, tag, pageable)));
    }


    @GetMapping("/{publicId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPost(@PathVariable UUID publicId,
                                                                   @VisitorId UUID visitorId) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getPublishedPost(publicId, visitorId)));
    }

    //////////// 어드민 ////////////
    // 어드민 전용 — 글 생성
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PostDetailResponse>> createPost(
            @Valid @RequestBody PostCreateRequest request) {
        PostDetailResponse created = postService.createPost(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> getAllPostsForAdmin(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getAllPostsForAdmin(status, keyword, pageable)));
    }

    @PatchMapping("/{publishId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PostDetailResponse>> updatePost(@Valid @RequestBody PostUpdateRequest request,
                                                                      @PathVariable UUID publishId) {
        return ResponseEntity.ok(ApiResponse.ok(postService.updatePost(publishId, request)));
    }

    @DeleteMapping("/{publishId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable UUID publishId) {
        postService.deletePost(publishId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PatchMapping("/{publishId}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> togglePublish(@PathVariable UUID publishId,
                                                           @RequestParam boolean isPublish) {
        postService.togglePublish(publishId, isPublish);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 어드민 전용 — DRAFT 포함 단건 조회
    @GetMapping("/admin/{publicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostForAdmin(@PathVariable UUID publicId) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getPostForAdmin(publicId)));
    }

}

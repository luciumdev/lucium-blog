package com.ysh.domain.comment.controller;

import com.ysh.common.response.ApiResponse;
import com.ysh.domain.comment.dto.CommentCreateRequest;
import com.ysh.domain.comment.dto.CommentDeleteRequest;
import com.ysh.domain.comment.dto.CommentResponse;
import com.ysh.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    /**
     * 댓글 목록 조회
     * @param publicId
     * @return
     */
    @GetMapping("/posts/{publicId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(@PathVariable UUID publicId) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.getComments(publicId)));
    }

    /**
     * 댓글 생성
     * @param publicId
     * @param request
     * @return
     */
    @PostMapping("/posts/{publicId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment (@PathVariable UUID publicId,
                                                                        @Valid @RequestBody CommentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        commentService.createComment(publicId, request)
                ));
    }

    /**
     * 댓글 삭제
     * @param commentPublicId
     * @param request
     * @return
     */
    @DeleteMapping("/comments/{commentPublicId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment (@PathVariable UUID commentPublicId,
                                                            @Valid @RequestBody CommentDeleteRequest request) {
        commentService.deleteComment(commentPublicId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 어드민 댓글 삭제
     * @param commentPublicId
     * @return
     */
    @DeleteMapping("/admin/comments/{commentPublicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminDeleteComment (@PathVariable UUID commentPublicId) {
        commentService.adminDeleteComment(commentPublicId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}

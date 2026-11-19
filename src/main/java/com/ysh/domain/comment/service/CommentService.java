package com.ysh.domain.comment.service;


import com.ysh.common.exception.BusinessException;
import com.ysh.common.exception.ErrorCode;
import com.ysh.domain.comment.dto.CommentCreateRequest;
import com.ysh.domain.comment.dto.CommentDeleteRequest;
import com.ysh.domain.comment.dto.CommentResponse;
import com.ysh.domain.comment.entity.Comment;
import com.ysh.domain.comment.repository.CommentRepository;
import com.ysh.domain.post.entity.Post;
import com.ysh.domain.post.entity.PostStatus;
import com.ysh.domain.post.repository.PostRepository;
import com.ysh.domain.post.service.PostService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final PasswordEncoder passwordEncoder;
    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID postPublicId) {
        Post post = postRepository.findByPublicIdAndStatus(postPublicId, PostStatus.PUBLISHED).orElseThrow(() ->
                new BusinessException(ErrorCode.POST_NOT_FOUND));
        List<Comment> comments = commentRepository.findAllByPostId(post.getId());
        return CommentResponse.toTree(comments);
    }

    @Transactional(readOnly = true)
    public CommentResponse createComment(UUID postPublicId, CommentCreateRequest request) {
        Post post = postRepository.findByPublicIdAndStatus(postPublicId, PostStatus.PUBLISHED).orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Comment parent = null;
        // 대댓글도 댓글 달 수 있게 변경
        if (request.getParentPublicId() != null) {
            parent = commentRepository.findByPublicId(request.getParentPublicId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
            if (parent.getParent() != null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "대댓글에는 답글을 달 수 없습니다.");
            }
        }
        Comment comment = Comment.builder()
                .post(post)
                .parent(parent)
                .authorName(request.getAuthorName())
                .password(passwordEncoder.encode(request.getPassword()))
                .content(request.getContent())
                .build();
        commentRepository.save(comment);
        return CommentResponse.from(comment);

    }

    @Transactional
    public void deleteComment(UUID publicId, CommentDeleteRequest request) {
        Comment comment = commentRepository.findByPublicId(publicId).orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        if (comment.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getPassword(), comment.getPassword())) {
            throw new BusinessException(ErrorCode.COMMENT_PASSWORD_INVALID);
        }
        comment.softDelete();
    }

    //// 어드민
    @Transactional
    public void adminDeleteComment(UUID publicId) {
        Comment comment = commentRepository.findByPublicId(publicId).orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        comment.softDelete();
    }


}

package com.ysh.domain.post.service;

import com.ysh.common.exception.BusinessException;
import com.ysh.common.exception.ErrorCode;
import com.ysh.domain.category.entity.Category;
import com.ysh.domain.category.repository.CategoryRepository;
import com.ysh.domain.post.dto.PostCreateRequest;
import com.ysh.domain.post.dto.PostDetailResponse;
import com.ysh.domain.post.dto.PostSummaryResponse;
import com.ysh.domain.post.dto.PostUpdateRequest;
import com.ysh.domain.post.entity.Post;
import com.ysh.domain.post.event.PostPublishedEvent;
import com.ysh.domain.post.event.PostUnpublishedEvent;
import com.ysh.domain.post.entity.PostStatus;
import com.ysh.domain.post.entity.PostTag;
import com.ysh.domain.post.repository.PostRepository;
import com.ysh.domain.tag.entity.Tag;
import com.ysh.domain.tag.repository.TagRepository;
import com.ysh.domain.user.entity.User;
import com.ysh.domain.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final ViewCountService viewCountService;

    //////////// 공통 ////////////
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPublishedPosts(String categorySlug,
                                                       String tagSlug,
                                                       Pageable pageable) {
        Page<Post> posts;
        if (StringUtils.hasText(tagSlug)) {
            posts = postRepository.findAllByTagSlug(tagSlug, pageable);
        } else if (StringUtils.hasText(categorySlug)) {
            posts = postRepository.findAllByCategorySlug(categorySlug, pageable);
        } else {
            posts = postRepository.findAllByStatus(PostStatus.PUBLISHED, pageable);
        }

        Map<Long, Long> buffered = viewCountService.getAllBufferedCounts();
        return posts.map(post -> {
            long live = post.getViewCount() + buffered.getOrDefault(post.getId(), 0L);
            return PostSummaryResponse.from(post, live);
        });
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getPublishedPost(UUID publicId, UUID visitorId) {
        Post post = postRepository
                .findByPublicIdAndStatus(publicId, PostStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        boolean alreadyRead = viewCountService.hasRead(visitorId, post.getId());

        viewCountService.tryIncrement(visitorId, post.getId());
        viewCountService.markRead(visitorId, post.getId());

        // DB 확정값 + 아직 flush 안 된 버퍼 잔량 = 실시간 조회수
        long liveViewCount = post.getViewCount()
                + viewCountService.getBufferedCount(post.getId());

        PostDetailResponse response = PostDetailResponse.from(post, liveViewCount);
        response.setAlreadyRead(alreadyRead);
        return response;
    }

    //////////// 어드민 ////////////
    /// post create
    @Transactional
    public PostDetailResponse createPost(PostCreateRequest request){
        User author = getCurrentUser();
        Category category= null;
        if (request.getCategoryId()!= null ) {
            category = categoryRepository.findById(request.getCategoryId()).orElseThrow(()->
                    new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        }
        Post post = Post.builder()
                .author(author)
                .category(category)
                .title(request.getTitle())
                .content(request.getContent())
                .contentFormat(request.getContentFormat())   // null → MARKDOWN (엔티티에서 처리)
                .thumbnail(request.getThumbnail())
                .summary(request.getSummary())
                .build();

        postRepository.save(post);
        attachTags(post, request.getTags());

        if (request.getStatus() == PostStatus.PUBLISHED) {
            post.publish();
            events.publishEvent(PostPublishedEvent.of(post));
        }
        return PostDetailResponse.from(post);
    }

    /// update post
    @Transactional
    public PostDetailResponse updatePost(UUID publicId, PostUpdateRequest request) {

        Post post = getPostByPublicId(publicId);

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        post.update(request.getTitle(), request.getContent(), request.getContentFormat(),
                    request.getSummary(), request.getThumbnail(), category);

        syncTags(post, request.getTags());

        if (post.isPublished()) {
            events.publishEvent(PostPublishedEvent.of(post));
        }

        return PostDetailResponse.from(post);
    }

    /**
     * 어드민 - DRAFT 포함 조회
     * @param publicId
     * @return
     */
    @Transactional(readOnly = true)
    public PostDetailResponse getPostForAdmin(UUID publicId) {
        Post post = getPostByPublicId(publicId);
        return PostDetailResponse.from(post);
    }

    @Transactional
    public void deletePost(UUID publicId) {
        Post post = getPostByPublicId(publicId);

        boolean wasPublished = post.isPublished();
        PostUnpublishedEvent event = wasPublished ? PostUnpublishedEvent.of(post) : null;

        // delete 이전에 태그 postCount 감소 — delete 후엔 컬렉션 접근이 불안정
        post.getPostTags().forEach(pt -> pt.getTag().decrementPostCount());

        postRepository.delete(post);

        if (event != null) {
            events.publishEvent(event);
        }
    }

    @Transactional
    public void togglePublish(UUID publicId, boolean publish) {
        Post post = getPostByPublicId(publicId);
        if (publish && !post.isPublished()) {
            post.publish();
            events.publishEvent(PostPublishedEvent.of(post));
        } else if (!publish && post.isPublished()) {
            post.unpublish();
            events.publishEvent(PostUnpublishedEvent.of(post));
        }
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getAllPostsForAdmin(
            PostStatus status,
            String keyword,
            Pageable pageable
    ) {
        String normalized = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Specification<Post> spec = (root, query, cb) -> {
            // category fetch — count query에선 fetch 호출 시 에러나므로 분기
            boolean isCountQuery = query != null
                    && (Long.class.equals(query.getResultType())
                    || long.class.equals(query.getResultType()));
            if (!isCountQuery) {
                root.fetch("category", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (normalized != null) {
                predicates.add(cb.like(
                        cb.lower(root.get("title")),
                        "%" + normalized.toLowerCase() + "%"
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return postRepository.findAll(spec, pageable).map(PostSummaryResponse::from);
    }

    // ── private ─────────────────────────────────────────────────

    private Post getPostByPublicId(UUID publicId) {
        return postRepository.findByPublicId(publicId)
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private void attachTags(Post post, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;

        tagNames.stream()
            .distinct()
            .map(name -> tagRepository.findByName(name)
                .orElseGet(() -> tagRepository.save(
                    Tag.builder().name(name).build())))
            .forEach(tag -> {
                tag.incrementPostCount();
                post.getPostTags().add(new PostTag(post, tag));
            });
    }

    /**
     * 태그 목록 조회해서 차집합만 add/remove
     *
     * @param post
     * @param tagNames
     */
    private void syncTags(Post post, List<String> tagNames) {
        Set<String> requested = (tagNames == null ? List.<String> of() : tagNames)
                .stream()
                .distinct()
                .collect(Collectors.toSet());

        // 제거 대상 Tag를 removeIf 이전에 캡처 → decrement 대상 확보
        List<Tag> toRemove = post.getPostTags().stream()
                .map(PostTag::getTag)
                .filter(tag -> !requested.contains(tag.getName()))
                .toList();
        toRemove.forEach(Tag::decrementPostCount);

        post.getPostTags()
                .removeIf(pt -> !requested.contains(pt.getTag().getName()));

        Set<String> existing = post.getPostTags().stream()
                .map(pt -> pt.getTag().getName())
                .collect(Collectors.toSet());

        Set<String> toAdd = new HashSet<>(requested);
        toAdd.removeAll(existing);
        toAdd.forEach(name ->{
            Tag tag = tagRepository.findByName(name)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
            tag.incrementPostCount();
            post.getPostTags().add(new PostTag(post, tag));
        });
    }

    /**
     * JWT principal에서 userId(UUID 문자열) 추출 → User 조회
     * JwtAuthenticationFilter에서 principal에 userId 저장
     */
    private User getCurrentUser() {
        String userId = (String) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        try {
            return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}

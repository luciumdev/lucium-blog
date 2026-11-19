package com.ysh.domain.stat.service;

import com.ysh.domain.post.dto.PostSummaryResponse;
import com.ysh.domain.post.entity.Post;
import com.ysh.domain.post.entity.PostStatus;
import com.ysh.domain.post.repository.PostRepository;
import com.ysh.domain.post.service.ViewCountService;
import com.ysh.domain.stat.dto.PostStatResponse;
import com.ysh.domain.stat.dto.SiteStatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 통계 read model — Post 집계 + Redis 조회수 버퍼 + 방문자 집계를 조립.
 * (게시글 데이터 소유는 post 도메인, 조립만 stat 도메인)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatService {

    private static final int POPULAR_MAX_SIZE = 10;
    /** DB 확정값 기준 후보를 넉넉히 뽑은 뒤 버퍼 반영 실시간 값으로 재정렬 */
    private static final int POPULAR_CANDIDATE_FACTOR = 2;

    private final PostRepository postRepository;
    private final ViewCountService viewCountService;
    private final VisitService visitService;

    /** 관리자 대시보드 — 발행/초안 개수 */
    public PostStatResponse getPostSummary() {
        long published = postRepository.countByStatus(PostStatus.PUBLISHED);
        long draft = postRepository.countByStatus(PostStatus.DRAFT);
        return PostStatResponse.of(published, draft);
    }

    /** 메인 프로필 카드 — 공개. 발행 글 수 + 오늘·누적 방문자 */
    public SiteStatResponse getSiteStat() {
        long published = postRepository.countByStatus(PostStatus.PUBLISHED);
        return new SiteStatResponse(published, visitService.today(), visitService.total());
    }

    /**
     * 많이 읽은 글 — 공개.
     * DB viewCount는 flush 주기만큼 늦으므로 후보를 2배수로 조회 후
     * 버퍼 잔량을 더한 실시간 조회수로 재정렬.
     */
    public List<PostSummaryResponse> getPopularPosts(int size) {
        int limit = Math.clamp(size, 1, POPULAR_MAX_SIZE);
        List<Post> candidates = postRepository
                .findTopByViewCount(PageRequest.of(0, limit * POPULAR_CANDIDATE_FACTOR))
                .getContent();

        Map<Long, Long> buffered = viewCountService.getAllBufferedCounts();
        return candidates.stream()
                .map(post -> PostSummaryResponse.from(
                        post, post.getViewCount() + buffered.getOrDefault(post.getId(), 0L)))
                .sorted(Comparator.comparingLong(PostSummaryResponse::getViewCount).reversed())
                .limit(limit)
                .toList();
    }
}

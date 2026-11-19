package com.ysh.infra.search;

import com.ysh.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    /**
     * 통합 검색 — title + content + summary 대상.
     *
     * 정렬: _score(관련도) 내림차순.
     * 추후 publishedAt 정렬 옵션 추가 시 SearchService 시그니처 확장.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SearchResponse>>> search(
            @RequestParam("q") String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(searchService.search(keyword, pageable)));
    }

}

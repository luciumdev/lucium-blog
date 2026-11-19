package com.ysh.infra.search;

import com.ysh.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/admin/search")
@RequiredArgsConstructor
public class ReindexController {
    private final ReindexService reindexService;

    /**
     * @param clear true면 기존 ES Document 전체 삭제 후 색인
     * 응답은 즉시 반환. 실제 백필은 reindexExecutor에서 비동기
     * 진행 상황은 서버 로그(스레드명 reindex-N)로 확인
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reindex(@RequestParam(defaultValue = "false") boolean clear) {
        boolean started = reindexService.tryStart(clear);
        return ResponseEntity.status(started ? HttpStatus.ACCEPTED : HttpStatus.CONFLICT)
                .body(ApiResponse.ok(Map.of(
                        "started", started,
                        "message", started ? "백필 시작" : "이미 백필 진행 중"
                )));
    }

    /**
     * 정합성 체크
     * @return
     */
    @GetMapping("/consistency")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReindexService.ConsistencyReport>> checkConsistency() {
        return ResponseEntity.ok(ApiResponse.ok(reindexService.checkConsistency()));
    }
}

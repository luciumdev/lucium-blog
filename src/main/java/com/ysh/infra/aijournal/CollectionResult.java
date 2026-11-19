package com.ysh.infra.aijournal;

import java.util.List;

public record CollectionResult(
        List<CollectedAiNews> news,
        List<SourceFailure> failures
) {
    public static CollectionResult empty() {
        return new CollectionResult(List.of(), List.of());
    }
}
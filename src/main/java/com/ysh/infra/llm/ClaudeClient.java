package com.ysh.infra.llm;

public interface ClaudeClient {
    String complete(ClaudePromptRequest request);
}

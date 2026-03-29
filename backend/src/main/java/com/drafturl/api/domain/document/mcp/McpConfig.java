package com.drafturl.api.domain.document.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider documentMcpToolCallbacks(DocumentMcpTools documentMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(documentMcpTools)
                .build();
    }
}

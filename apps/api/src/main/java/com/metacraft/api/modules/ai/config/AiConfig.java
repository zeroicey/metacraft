package com.metacraft.api.modules.ai.config;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.spring.event.AiServiceRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * AI 配置类
 * 用于监听 AI Service 注册事件，验证 tools 是否正确绑定
 */
@Slf4j
@Configuration
public class AiConfig implements ApplicationListener<AiServiceRegisteredEvent> {

    @Override
    public void onApplicationEvent(AiServiceRegisteredEvent event) {
        Class<?> aiServiceClass = event.aiServiceClass();
        log.info("========================================");
        log.info("AI Service Registered: {}", aiServiceClass.getSimpleName());
        log.info("========================================");

        var toolSpecifications = event.toolSpecifications();
        if (toolSpecifications.isEmpty()) {
            log.warn("No tools found for this AI Service!");
        } else {
            log.info("Registered {} tool(s):", toolSpecifications.size());
            for (int i = 0; i < toolSpecifications.size(); i++) {
                ToolSpecification tool = toolSpecifications.get(i);
                log.info("  [Tool-{}]: name='{}', description='{}'",
                        i + 1,
                        tool.name(),
                        tool.description());
            }
        }
        log.info("========================================");
    }
}

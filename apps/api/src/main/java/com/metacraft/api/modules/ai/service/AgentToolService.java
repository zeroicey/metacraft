package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.service.AppService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentToolService {

    private final AppService appService;

    @Tool("Save the generated application code, name, and description. Returns the preview URL.")
    public String saveApp(
            @P("The name of the application") String name,
            @P("A short description of the application") String description,
            @P("The complete HTML source code of the application") String code,
            @P("The user ID provided in the context") Long userId
    ) {
        // 1. 获取当前用户
        if (userId == null) {
            log.warn("User ID not provided, using default system user (1L)");
            userId = 1L;
        }
        
        log.info("Saving app for user {}: name={}, description={}", userId, name, description);

        // 2. 调用 AppService 保存应用
        AppEntity app = appService.createApp(userId, name, description);
        appService.createVersion(app.getId(), code, "Initial generation by AI");

        // 3. 返回预览链接
        String previewUrl = "/api/preview/" + app.getUuid() + "/v/1";
        return "应用已保存，预览链接：" + previewUrl;
    }
}

package com.metacraft.api.modules.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天响应")
public class ChatResponseVO {

    @Schema(description = "AI 回复的消息内容")
    private String reply;

    @Schema(description = "使用的模型名称")
    private String model;
}

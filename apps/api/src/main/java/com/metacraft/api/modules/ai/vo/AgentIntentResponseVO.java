package com.metacraft.api.modules.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "智能体意图响应")
public class AgentIntentResponseVO {

    @Schema(description = "识别出的类型: chat 或 gen")
    private String type;

    @Schema(description = "使用的模型名称")
    private String model;
}

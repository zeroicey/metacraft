package com.metacraft.api.modules._ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "智能体请求")
public class AgentRequestDTO {

    @Schema(description = "会话ID (可选，若不传则自动生成)", example = "uuid-string")
    private String sessionId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    @Schema(description = "用户消息内容", example = "请作为智能体规划任务")
    private String message;
}

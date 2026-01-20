package com.metacraft.api.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "智能体请求")
public class AgentRequestDTO {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    @Schema(description = "用户消息内容", example = "请作为智能体规划任务")
    private String message;
}

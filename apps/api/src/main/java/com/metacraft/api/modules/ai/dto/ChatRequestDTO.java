package com.metacraft.api.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "聊天请求")
public class ChatRequestDTO {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    @Schema(description = "用户消息内容", example = "你好,请介绍一下你自己")
    private String message;

    @Schema(description = "是否启用流式响应", example = "false")
    private Boolean stream = false;
}

package com.metacraft.api.modules._ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "生成 Logo 请求")
public class GenerateLogoRequestDTO {

    @NotBlank(message = "应用名称不能为空")
    @Size(max = 50, message = "应用名称不能超过50个字符")
    @Schema(description = "应用名称", example = "MetaCraft")
    private String name;

    @NotBlank(message = "应用描述不能为空")
    @Size(max = 500, message = "应用描述不能超过500个字符")
    @Schema(description = "应用描述", example = "一个创新的多端应用开发平台")
    private String description;
}

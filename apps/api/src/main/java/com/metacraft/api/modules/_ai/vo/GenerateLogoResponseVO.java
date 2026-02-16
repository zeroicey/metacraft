package com.metacraft.api.modules._ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "生成 Logo 响应")
public class GenerateLogoResponseVO {

    @Schema(description = "生成的图片 URL")
    private String imageUrl;

    @Schema(description = "图片的 Base64 编码(如果返回)")
    private String imageBase64;
}

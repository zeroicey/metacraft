package com.metacraft.api.modules._ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "智能体规划响应")
public class PlanResponseVO {

    @Schema(description = "规划内容")
    private String plan;

    @Schema(description = "使用的模型名称")
    private String model;
}

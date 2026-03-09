package com.metacraft.api.modules.yuanclaw.dto;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YuanClawWsMessage {
    private String type;
    private String chatId;
    private String senderId;
    private String content;
    private String replyTo;
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
package com.metacraft.api.modules.ai.service.pipeline;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.agent.ChatAgent;
import com.metacraft.api.modules.ai.dto.ChatMessageCreateDTO;
import com.metacraft.api.modules.ai.service.ChatMessageService;
import com.metacraft.api.modules.ai.util.SseUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPipelineService {

    private final ChatAgent chatAgent;
    private final SseUtils sseUtils;
    private final ChatMessageService chatMessageService;

    public Flux<ServerSentEvent<String>> execute(String message, String history, Long userId, String sessionId) {
        StringBuilder assistantReply = new StringBuilder();

        return chatAgent.chat(message, history)
                .doOnNext(assistantReply::append)
                .doOnComplete(() -> saveAssistantMessage(userId, sessionId, assistantReply.toString()))
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(sseUtils.toContentJson(chunk))
                        .build());
    }

    private void saveAssistantMessage(Long userId, String sessionId, String content) {
        if (content == null || content.isBlank()) {
            log.warn("Skip saving empty assistant message for session {}", sessionId);
            return;
        }

        ChatMessageCreateDTO assistantMessageDto = new ChatMessageCreateDTO();
        assistantMessageDto.setSessionId(sessionId);
        assistantMessageDto.setRole("assistant");
        assistantMessageDto.setContent(content);
        chatMessageService.saveMessage(userId, assistantMessageDto);
        log.info("Saved assistant message for session {}", sessionId);
    }
}

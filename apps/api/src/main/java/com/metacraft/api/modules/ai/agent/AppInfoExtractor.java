package com.metacraft.api.modules.ai.agent;

import com.metacraft.api.modules.ai.dto.AppInfoDTO;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface AppInfoExtractor {
	@SystemMessage(fromResource = "prompts/app-info.txt")
	@UserMessage("User requirement: {{it}}")
	AppInfoDTO extract(String message);
}

package com.metacraft.api.modules.ai.agent;

import com.metacraft.api.modules.ai.dto.AppCodeDTO;
import com.metacraft.api.modules.ai.dto.AppInfoDTO;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CodeGenerator {
    @SystemMessage(fromResource = "prompts/gen-code.txt")
    @UserMessage("Build an app with the following user requirement: {{requirement}}. App Info: {{info}}")
    AppCodeDTO generateCode(@V("requirement") String requirement, @V("info") AppInfoDTO info);
}
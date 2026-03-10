package com.metacraft.api.modules.ai.agent;

import com.metacraft.api.modules.ai.dto.AppCodeDiffDTO;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface CodeEditor {
    @SystemMessage(fromResource = "prompts/edit-code.txt")
    @UserMessage("""
            User edit request:
            {{requirement}}

            Current HTML:
            {{htmlCode}}

            Current JavaScript:
            {{jsCode}}
            """)
    AppCodeDiffDTO editCode(@V("requirement") String requirement, @V("htmlCode") String htmlCode,
            @V("jsCode") String jsCode);
}
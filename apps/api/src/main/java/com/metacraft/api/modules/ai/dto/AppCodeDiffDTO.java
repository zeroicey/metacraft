package com.metacraft.api.modules.ai.dto;

import dev.langchain4j.model.output.structured.Description;

public record AppCodeDiffDTO(
    @Description("Patch instructions for the HTML file only. Use zero or more SEARCH/REPLACE blocks inside a single string. Each block must follow this exact format: <<<<<<< SEARCH\n[exact original snippet]\n=======\n[replacement snippet]\n>>>>>>> REPLACE. Return an empty string if no HTML changes are needed.")
    String htmlDiff,

    @Description("Patch instructions for the JavaScript file only. Use zero or more SEARCH/REPLACE blocks inside a single string. Each block must follow this exact format: <<<<<<< SEARCH\n[exact original snippet]\n=======\n[replacement snippet]\n>>>>>>> REPLACE. Return an empty string if no JavaScript changes are needed.")
    String jsDiff
) {}
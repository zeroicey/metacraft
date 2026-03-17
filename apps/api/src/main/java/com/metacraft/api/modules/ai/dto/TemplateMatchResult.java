package com.metacraft.api.modules.ai.dto;

import com.metacraft.api.modules.ai.entity.Template;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateMatchResult {
    private boolean matched;
    private String templateName;
    private String reason;
    private Template template;

    public static TemplateMatchResult noMatch() {
        return new TemplateMatchResult(false, null, null, null);
    }

    public static TemplateMatchResult matched(Template template, String reason) {
        return new TemplateMatchResult(
            true,
            template.getFolderName(),
            reason,
            template
        );
    }
}
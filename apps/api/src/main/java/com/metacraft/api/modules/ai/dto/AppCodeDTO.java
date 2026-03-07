package com.metacraft.api.modules.ai.dto;

import dev.langchain4j.model.output.structured.Description;

public record AppCodeDTO(
    @Description("The complete HTML file content. It must include local Tailwind and Alpine scripts ('/public/css/tailwind.js' and '/public/js/alpine.js') and a script tag linking to 'app.js'. Do not use markdown formatting like ```html.")
    String htmlCode,

    @Description("The complete JavaScript logic for app.js. Keep custom business logic here and do not use markdown formatting like ```javascript.")
    String jsCode
) {}
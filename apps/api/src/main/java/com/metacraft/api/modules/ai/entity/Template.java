package com.metacraft.api.modules.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    private String folderName;        // 文件夹名
    private String appName;          // 应用名
    private String description;      // 描述
    private String htmlContent;      // index.html 内容
    private String jsContent;        // app.js 内容

    public static Template fromFolderName(String folderName) {
        Template template = new Template();
        template.setFolderName(folderName);

        // 解析文件夹名: {应用名}_{描述}
        int underscoreIndex = folderName.indexOf('_');
        if (underscoreIndex > 0) {
            template.setAppName(folderName.substring(0, underscoreIndex));
            template.setDescription(folderName.substring(underscoreIndex + 1));
        } else {
            template.setAppName(folderName);
            template.setDescription("");
        }

        return template;
    }
}
package com.metacraft.api.modules.yuanmeng.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "yuanmeng")
public class YuanMengProperties {
    private String sharedRoom = "shared";
    private String nanobotToken = "";
}
package com.metacraft.api.modules.yuanclaw.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "yuanclaw")
public class YuanClawProperties {
    private String sharedRoom = "shared";
    private String nanobotToken = "";
}
package com.metacraft.api.modules.ai.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "opencode")
public class OpenCodeProperties {
    private boolean enabled = true;
    private String command = "opencode";
    private String host = "127.0.0.1";
    private int port = 4096;
    private String username = "opencode";
    private String password = "change-this-opencode-password";
    private long startupTimeoutMillis = 15000;
    private long shutdownTimeoutMillis = 5000;
    private boolean inheritIo = true;

    public String getBaseUrl() {
        return "http://" + host + ":" + port;
    }

    public Duration getStartupTimeout() {
        return Duration.ofMillis(startupTimeoutMillis);
    }

    public Duration getShutdownTimeout() {
        return Duration.ofMillis(shutdownTimeoutMillis);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getStartupTimeoutMillis() {
        return startupTimeoutMillis;
    }

    public void setStartupTimeoutMillis(long startupTimeoutMillis) {
        this.startupTimeoutMillis = startupTimeoutMillis;
    }

    public long getShutdownTimeoutMillis() {
        return shutdownTimeoutMillis;
    }

    public void setShutdownTimeoutMillis(long shutdownTimeoutMillis) {
        this.shutdownTimeoutMillis = shutdownTimeoutMillis;
    }

    public boolean isInheritIo() {
        return inheritIo;
    }

    public void setInheritIo(boolean inheritIo) {
        this.inheritIo = inheritIo;
    }
}
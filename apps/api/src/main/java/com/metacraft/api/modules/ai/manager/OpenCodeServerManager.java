package com.metacraft.api.modules.ai.manager;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.metacraft.api.modules.ai.config.OpenCodeProperties;
import com.metacraft.api.modules.ai.dto.opencode.OpenCodeDtos;
import com.metacraft.api.modules.ai.service.opencode.OpenCodeClient;
import com.metacraft.api.modules.ai.service.opencode.OpenCodeWorkspaceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "opencode", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenCodeServerManager implements ApplicationRunner, DisposableBean {

    private final OpenCodeProperties properties;
    private final OpenCodeWorkspaceService workspaceService;
    private final OpenCodeClient openCodeClient;

    private volatile Process openCodeProcess;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (isServerAlreadyHealthy()) {
            log.info("OpenCode server already reachable at {}, skip spawning managed process", properties.getBaseUrl());
            return;
        }

        Path workspaceRoot = workspaceService.ensureWorkspaceRoot();
        log.info("Starting OpenCode server at {} with workspace {}", properties.getBaseUrl(), workspaceRoot);

        ProcessBuilder processBuilder = new ProcessBuilder(buildCommand());
        processBuilder.directory(workspaceRoot.toFile());
        processBuilder.environment().put("OPENCODE_SERVER_USERNAME", properties.getUsername());
        processBuilder.environment().put("OPENCODE_SERVER_PASSWORD", properties.getPassword());

        if (properties.isInheritIo()) {
            processBuilder.inheritIO();
        }

        try {
            openCodeProcess = processBuilder.start();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to start OpenCode server. Ensure the opencode CLI is installed and available in PATH.",
                    exception);
        }

        awaitHealthy();
    }

    @Override
    public void destroy() throws Exception {
        Process process = openCodeProcess;
        if (process == null || !process.isAlive()) {
            return;
        }

        log.info("Stopping managed OpenCode process");
        process.destroy();

        long deadline = System.currentTimeMillis() + properties.getShutdownTimeoutMillis();
        while (process.isAlive() && System.currentTimeMillis() < deadline) {
            sleep(200);
        }

        if (process.isAlive()) {
            log.warn("OpenCode process did not stop gracefully, forcing shutdown");
            process.destroyForcibly();
        }
    }

    private boolean isServerAlreadyHealthy() {
        try {
            OpenCodeDtos.HealthResponse healthResponse = openCodeClient.health();
            return healthResponse != null && healthResponse.healthy();
        } catch (Exception exception) {
            return false;
        }
    }

    private void awaitHealthy() {
        Instant deadline = Instant.now().plus(properties.getStartupTimeout());
        while (Instant.now().isBefore(deadline)) {
            if (openCodeProcess != null && !openCodeProcess.isAlive()) {
                throw new IllegalStateException("OpenCode process exited before becoming healthy. Exit code: "
                        + openCodeProcess.exitValue());
            }

            try {
                OpenCodeDtos.HealthResponse healthResponse = openCodeClient.health();
                if (healthResponse != null && healthResponse.healthy()) {
                    log.info("OpenCode server is healthy, version={}", healthResponse.version());
                    return;
                }
            } catch (Exception exception) {
                log.debug("Waiting for OpenCode server readiness: {}", exception.getMessage());
            }

            sleep(500);
        }

        throw new IllegalStateException("Timed out waiting for OpenCode server health endpoint: "
                + properties.getBaseUrl() + "/global/health");
    }

    private List<String> buildCommand() {
        List<String> command = new ArrayList<>();
        if (isWindows()) {
            command.add("cmd.exe");
            command.add("/c");
        }

        command.add(properties.getCommand());
        command.add("serve");
        command.add("--hostname");
        command.add(properties.getHost());
        command.add("--port");
        command.add(String.valueOf(properties.getPort()));
        return command;
    }

    private boolean isWindows() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase().contains("win");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for OpenCode server startup", exception);
        }
    }
}
package com.metacraft.api.modules.storage.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
@Service
public class StorageService {

    @Value("${app.storage.path:apps/data}")
    private String storagePath;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
            Files.createDirectories(this.rootLocation);
            log.info("Storage initialized at: {}", this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    /**
     * 保存文本文件
     *
     * @param relativePath 相对路径 (例如: apps/1/v1/index.html)
     * @param content      文本内容
     * @return 文件的绝对路径
     */
    public String saveTextFile(String relativePath, String content) {
        try {
            Path destinationFile = this.rootLocation.resolve(relativePath).normalize();
            
            // 安全检查：确保文件路径在存储目录下
            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new SecurityException("Cannot store file outside current directory.");
            }

            // 创建父目录
            Files.createDirectories(destinationFile.getParent());

            // 写入文件
            Files.write(destinationFile, content.getBytes(StandardCharsets.UTF_8), 
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            log.info("File saved to: {}", destinationFile);
            return destinationFile.toString();
        } catch (IOException e) {
            log.error("Failed to store file: {}", relativePath, e);
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    /**
     * 读取文本文件
     *
     * @param relativePath 相对路径
     * @return 文本内容
     */
    public String readTextFile(String relativePath) {
        try {
            Path file = this.rootLocation.resolve(relativePath).normalize();
            if (!file.startsWith(this.rootLocation)) {
                throw new SecurityException("Cannot read file outside current directory.");
            }
            if (Files.notExists(file)) {
                throw new RuntimeException("File not found: " + relativePath);
            }
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read file: {}", relativePath, e);
            throw new RuntimeException("Failed to read file.", e);
        }
    }
}

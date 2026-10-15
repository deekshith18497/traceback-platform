package com.lostfound.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StorageConfig {
    @Bean
    public Path uploadDirectory() {
        // On Render, set UPLOAD_DIR to the mount path of a persistent disk,
        // for example /var/data/uploads. Local development uses ./uploads.
        String configured = System.getenv().getOrDefault("UPLOAD_DIR", "uploads");
        Path path = Paths.get(configured).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
            return path;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create upload directory: " + path, exception);
        }
    }
}

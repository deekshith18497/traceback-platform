
package com.lostfound.service;

import com.lostfound.config.StorageConfig;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageService {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final Path uploadDirectory;

    public ImageService(StorageConfig storageConfig) {
        this.uploadDirectory = storageConfig.uploadDirectory();
    }

    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select an image");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Image size must not exceed 5 MB");
        }

        if (file.getContentType() == null
                || !ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "Only JPEG, PNG, and WebP images are allowed");
        }

        String originalFilename =
                StringUtils.cleanPath(
                        file.getOriginalFilename() == null
                                ? "image"
                                : file.getOriginalFilename());

        if (originalFilename.contains("..")
                || originalFilename.contains("/")
                || originalFilename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String extension = switch (file.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException(
                    "Unsupported image type");
        };

        Files.createDirectories(uploadDirectory);

        String filename = UUID.randomUUID() + extension;
        Path destination = uploadDirectory.resolve(filename).normalize();

        if (!destination.startsWith(uploadDirectory.normalize())) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        file.transferTo(destination);

        return "/uploads/" + filename;
    }
}

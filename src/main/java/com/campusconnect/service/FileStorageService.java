package com.campusconnect.service;

import com.campusconnect.config.AppProperties;
import com.campusconnect.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private static final Map<String, String> TYPES = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");
    private final AppProperties props;

    /** Stores an image and returns its relative URL, e.g. /uploads/uuid.jpg */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.bad("Please choose a photo");
        String ext = TYPES.get(file.getContentType());
        if (ext == null) throw ApiException.bad("Only JPG, PNG or WEBP photos are allowed");
        try {
            Path dir = Path.of(props.getUploadDir()).toAbsolutePath();
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + ext;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + name;
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not save photo");
        }
    }

    public void deleteQuietly(String relativeUrl) {
        try {
            if (relativeUrl != null && relativeUrl.startsWith("/uploads/")) {
                String name = Path.of(relativeUrl).getFileName().toString();
                Files.deleteIfExists(Path.of(props.getUploadDir()).toAbsolutePath().resolve(name));
            }
        } catch (Exception ignored) { }
    }
}

package com.campusconnect.service;

import com.campusconnect.config.AppProperties;
import com.campusconnect.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Development storage: saves into ./uploads and serves it at /uploads/** (see WebConfig). NOT for cloud hosts. */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {
    private final AppProperties props;

    @Override
    public String storeImage(MultipartFile file) {
        String ext = validateAndGetExtension(file);
        try {
            Path dir = Path.of(props.getUploadDir()).toAbsolutePath();
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + ext;           // random name: no user-controlled paths
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + name;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save photo");
        }
    }

    @Override
    public void deleteQuietly(String storedUrl) {
        try {
            if (storedUrl != null && storedUrl.startsWith("/uploads/")) {
                String name = Path.of(storedUrl).getFileName().toString();
                Files.deleteIfExists(Path.of(props.getUploadDir()).toAbsolutePath().resolve(name));
            }
        } catch (Exception ignored) { }
    }
}

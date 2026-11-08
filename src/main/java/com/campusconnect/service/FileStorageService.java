package com.campusconnect.service;

import com.campusconnect.exception.ApiException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Where profile photos live. The rest of the app only knows this interface (Strategy pattern):
 *  - LocalFileStorageService      -> files on this server's disk (development only)
 *  - CloudinaryFileStorageService -> files in Cloudinary (production; survives redeploys)
 * Chosen by the property app.storage.provider (env STORAGE_PROVIDER = local | cloudinary).
 *
 * The value returned by storeImage() is saved in the database: either "/uploads/x.jpg" (local)
 * or a full "https://res.cloudinary.com/..." URL. ProfileMapper.abs() handles both.
 */
public interface FileStorageService {

    /** Validates and stores the image, returns the URL/path to save in the database. */
    String storeImage(MultipartFile file);

    /** Best-effort delete; must never throw (a failed cleanup must not break the user's action). */
    void deleteQuietly(String storedUrl);

    /**
     * Security: the Content-Type header is controlled by the client, so we check the file's
     * real "magic bytes" instead. Returns the extension (.jpg/.png/.webp) or rejects the file.
     */
    default String validateAndGetExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.bad("Please choose a photo");
        try (InputStream in = file.getInputStream()) {
            byte[] h = in.readNBytes(12);
            if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return ".jpg";
            if (h.length >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return ".png";
            if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return ".webp";
        } catch (IOException e) {
            throw ApiException.bad("Could not read the photo");
        }
        throw ApiException.bad("Only real JPG, PNG or WEBP photos are allowed");
    }
}

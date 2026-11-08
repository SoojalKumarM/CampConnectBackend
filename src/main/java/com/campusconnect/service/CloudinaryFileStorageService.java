package com.campusconnect.service;

import com.campusconnect.config.AppProperties;
import com.campusconnect.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Production storage: uploads to Cloudinary through its REST API (no extra library needed).
 * Uploads are SIGNED: we sign the parameters with the API secret (SHA-1), so the secret never leaves the server.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "cloudinary")
public class CloudinaryFileStorageService implements FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(CloudinaryFileStorageService.class);
    /** https://res.cloudinary.com/<cloud>/image/upload/[v123/]folder/name.ext  ->  public id = folder/name */
    private static final Pattern PUBLIC_ID = Pattern.compile("/upload/(?:v\\d+/)?(.+)\\.[a-zA-Z0-9]+$");

    private final AppProperties.Cloudinary cfg;
    private final RestClient http = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    public CloudinaryFileStorageService(AppProperties props) { this.cfg = props.getStorage().getCloudinary(); }

    @PostConstruct
    void checkConfig() {   // fail fast at startup instead of failing on the first user upload
        if (blank(cfg.getCloudName()) || blank(cfg.getApiKey()) || blank(cfg.getApiSecret()))
            throw new IllegalStateException("STORAGE_PROVIDER=cloudinary but CLOUDINARY_CLOUD_NAME / CLOUDINARY_API_KEY / CLOUDINARY_API_SECRET are not all set");
    }

    @Override
    public String storeImage(MultipartFile file) {
        String ext = validateAndGetExtension(file);
        try {
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            Map<String, String> toSign = new TreeMap<>();
            toSign.put("folder", cfg.getFolder());
            toSign.put("timestamp", timestamp);

            ByteArrayResource part = new ByteArrayResource(file.getBytes()) {
                @Override public String getFilename() { return "photo" + ext; }
            };
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", part);
            body.add("api_key", cfg.getApiKey());
            body.add("timestamp", timestamp);
            body.add("folder", cfg.getFolder());
            body.add("signature", sign(toSign));

            String json = http.post().uri(endpoint("upload")).contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body).retrieve().body(String.class);
            JsonNode node = mapper.readTree(json);
            String url = node.path("secure_url").asText(null);
            if (url == null) throw new IllegalStateException("No secure_url in Cloudinary response");
            return url;
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw ApiException.bad("Could not read the photo");
        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Photo service is unavailable right now. Please try again.");
        }
    }

    @Override
    public void deleteQuietly(String storedUrl) {
        try {
            if (storedUrl == null || !storedUrl.contains("res.cloudinary.com")) return;
            Matcher m = PUBLIC_ID.matcher(storedUrl);
            if (!m.find()) return;
            String publicId = m.group(1);
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            Map<String, String> toSign = new TreeMap<>();
            toSign.put("public_id", publicId);
            toSign.put("timestamp", timestamp);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("public_id", publicId);
            form.add("timestamp", timestamp);
            form.add("api_key", cfg.getApiKey());
            form.add("signature", sign(toSign));
            http.post().uri(endpoint("destroy")).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("Could not delete image from Cloudinary: {}", e.getMessage());
        }
    }

    private String endpoint(String action) {
        return "https://api.cloudinary.com/v1_1/" + cfg.getCloudName() + "/image/" + action;
    }

    /** Cloudinary signature = hex(SHA-1("k1=v1&k2=v2" + apiSecret)), keys sorted alphabetically. */
    private String sign(Map<String, String> sortedParams) throws Exception {
        String data = sortedParams.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&")) + cfg.getApiSecret();
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
}

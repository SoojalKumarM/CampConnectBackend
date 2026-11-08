package com.campusconnect.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String frontendUrl;
    private String publicBaseUrl;
    private String uploadDir;
    private List<String> verifiedDomains = new ArrayList<>();
    private int dailyRequestLimit = 10;
    private int declineCooldownDays = 7;
    private Jwt jwt = new Jwt();
    private Google google = new Google();
    private Cors cors = new Cors();
    private Whatsapp whatsapp = new Whatsapp();
    private Mail mail = new Mail();
    private Storage storage = new Storage();

    @Data public static class Jwt { private String secret; private long expirationHours = 168; }
    @Data public static class Storage { private String provider = "local"; private Cloudinary cloudinary = new Cloudinary(); }
    @Data public static class Cloudinary { private String cloudName; private String apiKey; private String apiSecret; private String folder = "campus-connect"; }
    @Data public static class Mail { private boolean enabled = false; private String from; }
    @Data public static class Google { private String clientId; }
    @Data public static class Cors { private List<String> allowedOrigins = new ArrayList<>(); }
    @Data public static class Whatsapp {
        private String mode = "log";
        private String phoneNumberId;
        private String accessToken;
        private String templateName = "connection_request";
        private String language = "en";
    }
}

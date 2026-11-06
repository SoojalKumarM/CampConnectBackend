package com.campusconnect.service;

import com.campusconnect.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * PRIVACY DESIGN: the receiver's number never leaves the server before acceptance.
 * We send the heads-up from Campus Connect's official WhatsApp Business number (Meta Cloud API),
 * so the sender never needs (or sees) the receiver's number.
 * After acceptance both numbers are revealed and wa.me links (below) are used for real chatting.
 */
@Service
@RequiredArgsConstructor
public class WhatsAppService {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);
    private final AppProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    @Async
    public void notifyConnectionRequest(String receiverE164, String senderFirstName) {
        String link = props.getFrontendUrl() + "/requests";
        var cfg = props.getWhatsapp();
        if (!"cloud".equalsIgnoreCase(cfg.getMode())) {
            log.info("[WhatsApp DEV] to {}***: \"{} sent you a connection request on Campus Connect. Open {} to accept.\"",
                    receiverE164.substring(0, Math.min(5, receiverE164.length())), senderFirstName, link);
            return;
        }
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("messaging_product", "whatsapp");
            root.put("to", receiverE164.replace("+", ""));
            root.put("type", "template");
            ObjectNode tpl = root.putObject("template");
            tpl.put("name", cfg.getTemplateName());
            tpl.putObject("language").put("code", cfg.getLanguage());
            if (!"hello_world".equals(cfg.getTemplateName())) {   // hello_world = Meta's test template, no variables
                ArrayNode params = tpl.putArray("components").addObject().put("type", "body").putArray("parameters");
                params.addObject().put("type", "text").put("text", senderFirstName);
                params.addObject().put("type", "text").put("text", link);
            }

            HttpRequest req = HttpRequest.newBuilder(
                            URI.create("https://graph.facebook.com/v20.0/" + cfg.getPhoneNumberId() + "/messages"))
                    .header("Authorization", "Bearer " + cfg.getAccessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root))).build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300) log.warn("WhatsApp API error {}: {}", res.statusCode(), res.body());
            else log.info("WhatsApp message accepted by Meta: {}", res.body());
        } catch (Exception e) {
            log.warn("WhatsApp notification failed (request still created): {}", e.getMessage());
        }
    }

    /** Click-to-chat link with a friendly prefilled message. Only generated for CONNECTED users. */
    public static String chatLink(String e164, String myName) {
        String text = "Hi! It's " + myName + " from Campus Connect. Glad we connected \uD83D\uDC4B";
        return "https://wa.me/" + e164.replace("+", "") + "?text="
                + URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

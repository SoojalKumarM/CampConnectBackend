package com.campusconnect.service;

import com.campusconnect.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * FREE replacement for the paid WhatsApp heads-up. The receiver's account email is used,
 * and the sender never sees it, so privacy is unchanged.
 */
@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final AppProperties props;
    @Value("${spring.mail.username:}") private String smtpUser;

    @Async
    public void sendRequestReceived(String to, String senderFirstName, String note) {
        String body = "Hi!\n\n" + senderFirstName + " sent you a connection request on Campus Connect."
                + (note != null && !note.isBlank() ? "\n\nTheir note: \"" + note + "\"" : "")
                + "\n\nOpen the app to view their profile and accept or decline:\n"
                + props.getFrontendUrl() + "/requests"
                + "\n\nYour WhatsApp number stays private until you accept.\n\n— Campus Connect";
        send(to, senderFirstName + " wants to connect with you on Campus Connect", body);
    }

    @Async
    public void sendRequestAccepted(String to, String otherFirstName) {
        String body = "Great news!\n\n" + otherFirstName + " accepted your connection request. "
                + "Their WhatsApp number is now visible to you:\n" + props.getFrontendUrl() + "/connections"
                + "\n\n— Campus Connect";
        send(to, otherFirstName + " accepted your request \uD83C\uDF89", body);
    }

    private void send(String to, String subject, String text) {
        if (!props.getMail().isEnabled()) {
            log.info("[EMAIL DEV] to={} subject=\"{}\"\n{}", to, subject, text);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            String from = props.getMail().getFrom();
            m.setFrom(from != null && !from.isBlank() ? from : smtpUser);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(text);
            mailSender.send(m);
        } catch (Exception e) {
            log.warn("Email failed (request still created): {}", e.getMessage());
        }
    }
}

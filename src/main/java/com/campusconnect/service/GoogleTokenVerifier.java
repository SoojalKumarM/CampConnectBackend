package com.campusconnect.service;

import com.campusconnect.config.AppProperties;
import com.campusconnect.exception.ApiException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/** Verifies the Google ID token (the "credential" sent by the React Google button). */
@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier {
    private final AppProperties props;
    private volatile GoogleIdTokenVerifier verifier;

    public record GoogleUser(String sub, String email, boolean emailVerified, String name) {}

    public GoogleUser verify(String credential) {
        String clientId = props.getGoogle().getClientId();
        if (clientId == null || clientId.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Google sign-in is not configured on the server (GOOGLE_CLIENT_ID is missing).");
        }
        try {
            if (verifier == null) {
                verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(List.of(clientId)).build();
            }
            GoogleIdToken token = verifier.verify(credential);
            if (token == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED,
                        "Google token rejected. Make sure the SAME Google Client ID is used in frontend and backend.");
            }
            GoogleIdToken.Payload p = token.getPayload();
            return new GoogleUser(p.getSubject(), p.getEmail(), Boolean.TRUE.equals(p.getEmailVerified()),
                    (String) p.get("name"));
        } catch (GeneralSecurityException | IOException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not verify Google token: " + e.getMessage());
        }
    }
}

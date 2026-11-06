package com.campusconnect.service;

import com.campusconnect.config.AppProperties;
import com.campusconnect.dto.Dtos.*;
import com.campusconnect.entity.Profile;
import com.campusconnect.entity.User;
import com.campusconnect.exception.ApiException;
import com.campusconnect.repository.ProfileRepository;
import com.campusconnect.repository.UserRepository;
import com.campusconnect.security.JwtService;
import com.campusconnect.util.PhoneUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final GoogleTokenVerifier google;
    private final ProfileMapper mapper;
    private final AppProperties props;

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw ApiException.conflict("An account with this email already exists. Try logging in.");
        String phone = PhoneUtil.normalize(r.whatsappNumber());
        if (users.existsByWhatsappNumber(phone)) throw ApiException.conflict("This WhatsApp number is already registered.");

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setWhatsappNumber(phone);
        u.setVerifiedStudent(isUniversityEmail(email));
        users.save(u);
        Profile p = newProfile(u, r.fullName().trim());
        return response(u, p);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));
        if (!u.isActive()) throw ApiException.forbidden("This account has been deactivated");
        if (u.getPasswordHash() == null)
            throw new ApiException(HttpStatus.UNAUTHORIZED, "This account uses Google sign-in. Please use 'Continue with Google'.");
        if (!encoder.matches(r.password(), u.getPasswordHash()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        return response(u, profiles.findByUserId(u.getId()).orElseThrow());
    }

    /** One endpoint for Google login AND Google sign-up (creates the account on first use). */
    @Transactional
    public AuthResponse google(String credential) {
        GoogleTokenVerifier.GoogleUser g = google.verify(credential);
        if (!g.emailVerified()) throw ApiException.bad("Your Google email is not verified");
        String email = g.email().toLowerCase(Locale.ROOT);

        User u = users.findByGoogleId(g.sub()).orElse(null);
        if (u == null) u = users.findByEmail(email).orElse(null);
        if (u == null) {
            u = new User();
            u.setEmail(email);
            u.setVerifiedStudent(isUniversityEmail(email));
            u.setGoogleId(g.sub());
            users.save(u);
            newProfile(u, g.name() != null ? g.name() : email.substring(0, email.indexOf('@')));
        } else {
            if (!u.isActive()) throw ApiException.forbidden("This account has been deactivated");
            if (u.getGoogleId() == null) u.setGoogleId(g.sub());   // link existing email account
        }
        return response(u, profiles.findByUserId(u.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public MeDto me(Long userId) {
        User u = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account not found"));
        return mapper.toMe(u, profiles.findByUserId(userId).orElseThrow());
    }

    private Profile newProfile(User u, String name) {
        Profile p = new Profile();
        p.setUser(u);
        p.setFullName(name);
        return profiles.save(p);
    }

    private AuthResponse response(User u, Profile p) {
        return new AuthResponse(jwt.generate(u.getId()), mapper.toMe(u, p));
    }

    private boolean isUniversityEmail(String email) {
        return props.getVerifiedDomains().stream().map(String::trim).filter(s -> !s.isEmpty())
                .anyMatch(d -> email.endsWith("@" + d.toLowerCase(Locale.ROOT)) || email.endsWith("." + d.toLowerCase(Locale.ROOT)));
    }
}

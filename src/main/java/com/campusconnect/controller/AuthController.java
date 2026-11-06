package com.campusconnect.controller;

import com.campusconnect.dto.Dtos.*;
import com.campusconnect.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok"); }

    @PostMapping("/auth/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) { return auth.register(r); }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }

    @PostMapping("/auth/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest r) { return auth.google(r.credential()); }

    @GetMapping("/auth/me")
    public MeDto me(@AuthenticationPrincipal Long userId) { return auth.me(userId); }
}

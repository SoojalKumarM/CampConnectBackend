package com.campusconnect.controller;

import com.campusconnect.dto.Dtos.*;
import com.campusconnect.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService service;

    @GetMapping("/profile/me")
    public MyProfileDto mine(@AuthenticationPrincipal Long me) { return service.getMine(me); }

    @PutMapping("/profile/me")
    public MyProfileDto update(@AuthenticationPrincipal Long me, @Valid @RequestBody ProfileUpdateRequest r) {
        return service.update(me, r);
    }

    @PostMapping("/profile/me/complete")
    public MeDto complete(@AuthenticationPrincipal Long me) { return service.complete(me); }

    @PostMapping(value = "/profile/me/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MyProfileDto addPhoto(@AuthenticationPrincipal Long me, @RequestParam("file") MultipartFile file) {
        return service.addPhoto(me, file);
    }

    @DeleteMapping("/profile/me/photos/{index}")
    public MyProfileDto removePhoto(@AuthenticationPrincipal Long me, @PathVariable int index) {
        return service.removePhoto(me, index);
    }

    @PutMapping("/profile/me/photos/{index}/primary")
    public MyProfileDto primary(@AuthenticationPrincipal Long me, @PathVariable int index) {
        return service.makePrimary(me, index);
    }

    @GetMapping("/profiles/{userId}")
    public ProfileDto view(@AuthenticationPrincipal Long me, @PathVariable Long userId) {
        return service.view(me, userId);
    }

    @PutMapping("/account/visibility")
    public MeDto visibility(@AuthenticationPrincipal Long me, @RequestBody VisibilityRequest r) {
        return service.setVisibility(me, r.visible());
    }

    @DeleteMapping("/account")
    public void deactivate(@AuthenticationPrincipal Long me) { service.deactivate(me); }
}

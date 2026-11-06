package com.campusconnect.controller;

import com.campusconnect.dto.Dtos.ProfileDto;
import com.campusconnect.dto.Dtos.ReportRequest;
import com.campusconnect.service.SafetyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety")
@RequiredArgsConstructor
public class SafetyController {
    private final SafetyService service;

    @PostMapping("/block/{userId}") public void block(@AuthenticationPrincipal Long me, @PathVariable Long userId) { service.block(me, userId); }
    @DeleteMapping("/block/{userId}") public void unblock(@AuthenticationPrincipal Long me, @PathVariable Long userId) { service.unblock(me, userId); }
    @GetMapping("/blocked") public List<ProfileDto> blocked(@AuthenticationPrincipal Long me) { return service.blocked(me); }
    @PostMapping("/report") public void report(@AuthenticationPrincipal Long me, @Valid @RequestBody ReportRequest r) { service.report(me, r); }
}

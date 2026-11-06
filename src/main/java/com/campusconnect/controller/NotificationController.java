package com.campusconnect.controller;

import com.campusconnect.dto.Dtos.NotificationDto;
import com.campusconnect.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @GetMapping public List<NotificationDto> list(@AuthenticationPrincipal Long me) { return service.list(me); }
    @PostMapping("/read-all") public void readAll(@AuthenticationPrincipal Long me) { service.markAllRead(me); }
    @PostMapping("/{id}/read") public void read(@AuthenticationPrincipal Long me, @PathVariable Long id) { service.markRead(me, id); }
}

package com.campusconnect.controller;

import com.campusconnect.dto.Dtos.*;
import com.campusconnect.service.ConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {
    private final ConnectionService service;

    @PostMapping("/request/{targetUserId}")
    public ConnectionDto send(@AuthenticationPrincipal Long me, @PathVariable Long targetUserId,
                              @Valid @RequestBody(required = false) SendRequestBody body) {
        return service.send(me, targetUserId, body == null ? null : body.message());
    }

    @GetMapping("/received") public List<ConnectionDto> received(@AuthenticationPrincipal Long me) { return service.received(me); }
    @GetMapping("/sent") public List<ConnectionDto> sent(@AuthenticationPrincipal Long me) { return service.sent(me); }
    @GetMapping public List<ConnectionDto> connections(@AuthenticationPrincipal Long me) { return service.connections(me); }
    @GetMapping("/summary") public SummaryDto summary(@AuthenticationPrincipal Long me) { return service.summary(me); }

    @PostMapping("/{id}/accept")
    public ConnectionDto accept(@AuthenticationPrincipal Long me, @PathVariable Long id) { return service.respond(me, id, true); }

    @PostMapping("/{id}/decline")
    public ConnectionDto decline(@AuthenticationPrincipal Long me, @PathVariable Long id) { return service.respond(me, id, false); }

    @DeleteMapping("/{id}/cancel")
    public void cancel(@AuthenticationPrincipal Long me, @PathVariable Long id) { service.cancel(me, id); }

    @DeleteMapping("/{id}")
    public void remove(@AuthenticationPrincipal Long me, @PathVariable Long id) { service.removeConnection(me, id); }
}

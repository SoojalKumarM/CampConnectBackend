package com.campusconnect.controller;

import com.campusconnect.dto.Dtos.PageDto;
import com.campusconnect.dto.Dtos.ProfileDto;
import com.campusconnect.entity.Gender;
import com.campusconnect.service.DiscoverService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discover")
@RequiredArgsConstructor
public class DiscoverController {
    private final DiscoverService service;

    @GetMapping
    public PageDto<ProfileDto> discover(@AuthenticationPrincipal Long me,
                                        @RequestParam(required = false) Gender gender,
                                        @RequestParam(required = false) String university,
                                        @RequestParam(required = false) String department,
                                        @RequestParam(required = false) Integer year,
                                        @RequestParam(required = false) String interest,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(defaultValue = "0") int page) {
        return service.discover(me, gender, university, department, year, interest, q, page);
    }

    @GetMapping("/top")
    public List<ProfileDto> top(@AuthenticationPrincipal Long me) { return service.topMatches(me); }
}

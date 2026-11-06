package com.campusconnect.service;

import com.campusconnect.dto.Dtos.PageDto;
import com.campusconnect.dto.Dtos.ProfileDto;
import com.campusconnect.entity.ConnectionRequest;
import com.campusconnect.entity.Gender;
import com.campusconnect.entity.Profile;
import com.campusconnect.exception.ApiException;
import com.campusconnect.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DiscoverService {
    private static final int PAGE_SIZE = 12;
    private final ProfileRepository profiles;
    private final ProfileMapper mapper;

    @Transactional(readOnly = true)
    public PageDto<ProfileDto> discover(Long me, Gender gender, String university, String department,
                                        Integer year, String interest, String q, int page) {
        Profile mine = profiles.findByUserId(me).orElseThrow(() -> ApiException.notFound("Profile not found"));
        Page<Profile> result = profiles.discover(me, gender, blankToNull(university), blankToNull(department),
                year, blankToNull(interest), blankToNull(q),
                PageRequest.of(Math.max(0, page), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id")));
        Map<Long, ConnectionRequest> rels = mapper.relations(me);
        List<ProfileDto> content = result.getContent().stream()
                .map(p -> mapper.toDto(p, mine, rels.get(p.getUser().getId()))).toList();
        return new PageDto<>(content, result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }

    /** Top matches: best shared-interest score among people you haven't interacted with yet. */
    @Transactional(readOnly = true)
    public List<ProfileDto> topMatches(Long me) {
        Profile mine = profiles.findByUserId(me).orElseThrow(() -> ApiException.notFound("Profile not found"));
        Map<Long, ConnectionRequest> rels = mapper.relations(me);
        return profiles.discover(me, null, null, null, null, null, null, PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "id")))
                .getContent().stream()
                .filter(p -> !rels.containsKey(p.getUser().getId()))
                .map(p -> mapper.toDto(p, mine, null))
                .sorted(Comparator.comparing((ProfileDto d) -> d.compatibility() == null ? 0 : d.compatibility()).reversed())
                .limit(12).toList();
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}

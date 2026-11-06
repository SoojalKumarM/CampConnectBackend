package com.campusconnect.service;

import com.campusconnect.config.AppProperties;
import com.campusconnect.dto.Dtos.MeDto;
import com.campusconnect.dto.Dtos.MyProfileDto;
import com.campusconnect.dto.Dtos.ProfileDto;
import com.campusconnect.entity.*;
import com.campusconnect.repository.ConnectionRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ProfileMapper {
    private final AppProperties props;
    private final ConnectionRequestRepository requests;

    public String abs(String relative) {
        if (relative == null) return null;
        return relative.startsWith("http") ? relative : props.getPublicBaseUrl() + relative;
    }

    public List<String> absAll(List<String> rel) { return rel.stream().map(this::abs).toList(); }

    /** Active (PENDING / ACCEPTED) requests keyed by the OTHER user's id. */
    public Map<Long, ConnectionRequest> relations(Long me) {
        Map<Long, ConnectionRequest> map = new HashMap<>();
        for (ConnectionRequest r : requests.activeFor(me, List.of(RequestStatus.PENDING, RequestStatus.ACCEPTED))) {
            Long other = r.getSender().getId().equals(me) ? r.getReceiver().getId() : r.getSender().getId();
            map.put(other, r);
        }
        return map;
    }

    public MeDto toMe(User u, Profile p) {
        return new MeDto(u.getId(), u.getEmail(), p.getFullName(),
                p.getPhotos().isEmpty() ? null : abs(p.getPhotos().get(0)),
                u.getWhatsappNumber() != null, u.isVerifiedStudent(), u.isProfileComplete(), p.isVisible());
    }

    public MyProfileDto toMine(User u, Profile p) {
        return new MyProfileDto(u.getId(), u.getEmail(), p.getFullName(), p.getBirthDate(), p.getGender(),
                p.getLookingFor(), p.getUniversity(), p.getDepartment(), p.getYearOfStudy(), p.getBio(),
                new ArrayList<>(p.getInterests()), absAll(p.getPhotos()), u.getWhatsappNumber(),
                u.isVerifiedStudent(), u.isProfileComplete(), p.isVisible());
    }

    /** Public view of `target` as seen by `viewer`. `rel` is the active request between them (or null). */
    public ProfileDto toDto(Profile target, Profile viewer, ConnectionRequest rel) {
        Long viewerId = viewer.getUser().getId();
        Long targetId = target.getUser().getId();
        String relation = "NONE";
        Long requestId = null;
        if (targetId.equals(viewerId)) {
            relation = "SELF";
        } else if (rel != null) {
            requestId = rel.getId();
            if (rel.getStatus() == RequestStatus.ACCEPTED) relation = "CONNECTED";
            else if (rel.getStatus() == RequestStatus.PENDING)
                relation = rel.getSender().getId().equals(viewerId) ? "REQUEST_SENT" : "REQUEST_RECEIVED";
        }

        List<String> shared = sharedInterests(target, viewer);
        Integer compat = relation.equals("SELF") ? null : compatibility(target, viewer, shared.size());

        String wa = null, waLink = null;
        if (relation.equals("CONNECTED") && target.getUser().getWhatsappNumber() != null) {
            wa = target.getUser().getWhatsappNumber();
            waLink = WhatsAppService.chatLink(wa, viewer.getFullName());
        }
        return new ProfileDto(targetId, target.getFullName(), age(target.getBirthDate()), target.getGender(),
                target.getLookingFor(), target.getUniversity(), target.getDepartment(), target.getYearOfStudy(),
                target.getBio(), new ArrayList<>(target.getInterests()), absAll(target.getPhotos()),
                target.getUser().isVerifiedStudent(), compat, shared, relation, requestId, wa, waLink);
    }

    public static Integer age(LocalDate birth) {
        return birth == null ? null : Period.between(birth, LocalDate.now()).getYears();
    }

    private List<String> sharedInterests(Profile a, Profile b) {
        Set<String> mine = new HashSet<>();
        b.getInterests().forEach(i -> mine.add(i.toLowerCase()));
        return a.getInterests().stream().filter(i -> mine.contains(i.toLowerCase())).toList();
    }

    /** Simple, explainable score: shared interests (80%) + same intent (10) + same university (10). */
    public int compatibility(Profile a, Profile b, int sharedCount) {
        int smaller = Math.max(1, Math.min(a.getInterests().size(), b.getInterests().size()));
        double score = Math.min(1.0, sharedCount / (double) smaller) * 80;
        if (a.getLookingFor() != null && a.getLookingFor() == b.getLookingFor()) score += 10;
        if (a.getUniversity() != null && a.getUniversity().equalsIgnoreCase(b.getUniversity())) score += 10;
        return (int) Math.min(100, Math.round(score));
    }
}

package com.campusconnect.service;

import com.campusconnect.dto.Dtos.ProfileDto;
import com.campusconnect.dto.Dtos.ReportRequest;
import com.campusconnect.entity.*;
import com.campusconnect.exception.ApiException;
import com.campusconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SafetyService {
    private final BlockRepository blocks;
    private final ReportRepository reports;
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final ConnectionRequestRepository requests;
    private final ProfileMapper mapper;

    @Transactional
    public void block(Long me, Long target) {
        if (me.equals(target)) throw ApiException.bad("You can't block yourself");
        User blocker = users.findById(me).orElseThrow();
        User blocked = users.findById(target).orElseThrow(() -> ApiException.notFound("User not found"));
        if (!blocks.existsByBlockerIdAndBlockedId(me, target)) {
            Block b = new Block();
            b.setBlocker(blocker); b.setBlocked(blocked);
            blocks.save(b);
        }
        // Blocking ends any pending request / connection -> their number is hidden again.
        for (ConnectionRequest r : requests.between(me, target)) {
            if (r.getStatus() == RequestStatus.PENDING) r.setStatus(RequestStatus.CANCELLED);
            else if (r.getStatus() == RequestStatus.ACCEPTED) r.setStatus(RequestStatus.REMOVED);
            else continue;
            r.setRespondedAt(Instant.now());
        }
    }

    @Transactional
    public void unblock(Long me, Long target) { blocks.deleteByBlockerIdAndBlockedId(me, target); }

    @Transactional(readOnly = true)
    public List<ProfileDto> blocked(Long me) {
        Profile mine = profiles.findByUserId(me).orElseThrow();
        return blocks.findByBlockerIdOrderByCreatedAtDesc(me).stream()
                .map(b -> profiles.findByUserId(b.getBlocked().getId()).orElse(null))
                .filter(p -> p != null).map(p -> mapper.toDto(p, mine, null)).toList();
    }

    @Transactional
    public void report(Long me, ReportRequest r) {
        if (me.equals(r.userId())) throw ApiException.bad("You can't report yourself");
        Report rep = new Report();
        rep.setReporter(users.findById(me).orElseThrow());
        rep.setReported(users.findById(r.userId()).orElseThrow(() -> ApiException.notFound("User not found")));
        rep.setReason(r.reason());
        rep.setDetails(r.details());
        reports.save(rep);
        block(me, r.userId());     // reporting also blocks, so you never see them again
    }
}

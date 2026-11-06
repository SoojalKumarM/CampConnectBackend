package com.campusconnect.service;

import com.campusconnect.config.AppProperties;
import com.campusconnect.dto.Dtos.ConnectionDto;
import com.campusconnect.dto.Dtos.SummaryDto;
import com.campusconnect.entity.*;
import com.campusconnect.exception.ApiException;
import com.campusconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConnectionService {
    private final ConnectionRequestRepository repo;
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final BlockRepository blocks;
    private final NotificationRepository notificationRepo;
    private final NotificationService notifications;
    private final WhatsAppService whatsApp;
    private final EmailService email;
    private final ProfileMapper mapper;
    private final AppProperties props;

    // ---------------------------------------------------------------- send
    @Transactional
    public ConnectionDto send(Long me, Long targetId, String message) {
        if (me.equals(targetId)) throw ApiException.bad("You can't connect with yourself");
        User sender = users.findById(me).orElseThrow(() -> ApiException.notFound("Account not found"));
        if (!sender.isProfileComplete()) throw ApiException.bad("Complete your profile before sending requests");
        User receiver = users.findById(targetId).filter(u -> u.isActive() && u.isProfileComplete())
                .orElseThrow(() -> ApiException.notFound("This profile isn't available"));
        if (blocks.blockedEitherWay(me, targetId)) throw ApiException.notFound("This profile isn't available");

        List<ConnectionRequest> history = repo.between(me, targetId);
        for (ConnectionRequest r : history) {
            if (r.getStatus() == RequestStatus.ACCEPTED) throw ApiException.conflict("You're already connected");
            if (r.getStatus() == RequestStatus.PENDING) {
                if (r.getSender().getId().equals(me)) throw ApiException.conflict("You already sent a request to this person");
                return accept(r);                      // they liked you first -> instant match
            }
        }
        Instant cooldownStart = Instant.now().minus(props.getDeclineCooldownDays(), ChronoUnit.DAYS);
        boolean coolingDown = history.stream().anyMatch(r -> r.getStatus() == RequestStatus.DECLINED
                && r.getSender().getId().equals(me) && r.getRespondedAt() != null && r.getRespondedAt().isAfter(cooldownStart));
        if (coolingDown) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "You can try connecting with this person again after " + props.getDeclineCooldownDays() + " days.");

        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (repo.countBySenderIdAndCreatedAtAfter(me, startOfDay) >= props.getDailyRequestLimit())
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Daily limit of " + props.getDailyRequestLimit() + " requests reached. Quality over quantity — try again tomorrow!");

        ConnectionRequest r = new ConnectionRequest();
        r.setSender(sender);
        r.setReceiver(receiver);
        r.setMessage(message == null || message.isBlank() ? null : message.trim());
        repo.save(r);

        Profile sp = profiles.findByUserId(me).orElseThrow();
        String firstName = sp.getFullName().split(" ")[0];
        notifications.create(receiver, "REQUEST_RECEIVED", firstName + " wants to connect",
                r.getMessage() != null ? r.getMessage() : "Open the request to view their profile.", "/requests");
        // Heads-up on WhatsApp (sent from Campus Connect's number; receiver's number stays private)
        email.sendRequestReceived(receiver.getEmail(), firstName, r.getMessage());          // free heads-up
        if (receiver.getWhatsappNumber() != null) whatsApp.notifyConnectionRequest(receiver.getWhatsappNumber(), firstName);  // optional/paid, off by default
        return toDto(r, me);
    }

    // ------------------------------------------------------------- respond
    @Transactional
    public ConnectionDto respond(Long me, Long requestId, boolean accept) {
        ConnectionRequest r = find(requestId);
        if (!r.getReceiver().getId().equals(me)) throw ApiException.forbidden("This request wasn't sent to you");
        if (r.getStatus() != RequestStatus.PENDING) throw ApiException.conflict("This request was already handled");
        if (accept) return accept(r);
        r.setStatus(RequestStatus.DECLINED);
        r.setRespondedAt(Instant.now());
        return toDto(r, me);
    }

    private ConnectionDto accept(ConnectionRequest r) {
        r.setStatus(RequestStatus.ACCEPTED);
        r.setRespondedAt(Instant.now());
        Profile receiverProfile = profiles.findByUserId(r.getReceiver().getId()).orElseThrow();
        notifications.create(r.getSender(), "REQUEST_ACCEPTED",
                receiverProfile.getFullName().split(" ")[0] + " accepted your request \uD83C\uDF89",
                "Their WhatsApp number is now visible in Connections.", "/connections");
        email.sendRequestAccepted(r.getSender().getEmail(), receiverProfile.getFullName().split(" ")[0]);
        notifications.create(r.getReceiver(), "REQUEST_ACCEPTED", "You're now connected",
                "Their WhatsApp number is now visible in Connections.", "/connections");
        return toDto(r, r.getReceiver().getId());
    }

    @Transactional
    public void cancel(Long me, Long requestId) {
        ConnectionRequest r = find(requestId);
        if (!r.getSender().getId().equals(me)) throw ApiException.forbidden("Not your request");
        if (r.getStatus() != RequestStatus.PENDING) throw ApiException.conflict("This request can't be cancelled anymore");
        r.setStatus(RequestStatus.CANCELLED);
        r.setRespondedAt(Instant.now());
    }

    @Transactional
    public void removeConnection(Long me, Long requestId) {
        ConnectionRequest r = find(requestId);
        boolean participant = r.getSender().getId().equals(me) || r.getReceiver().getId().equals(me);
        if (!participant) throw ApiException.forbidden("Not your connection");
        if (r.getStatus() != RequestStatus.ACCEPTED) throw ApiException.conflict("Not connected");
        r.setStatus(RequestStatus.REMOVED);
        r.setRespondedAt(Instant.now());
    }

    // --------------------------------------------------------------- lists
    @Transactional(readOnly = true)
    public List<ConnectionDto> received(Long me) {
        return repo.findByReceiverIdAndStatusOrderByCreatedAtDesc(me, RequestStatus.PENDING).stream().map(r -> toDto(r, me)).toList();
    }

    @Transactional(readOnly = true)
    public List<ConnectionDto> sent(Long me) {
        return repo.findBySenderIdAndStatusOrderByCreatedAtDesc(me, RequestStatus.PENDING).stream().map(r -> toDto(r, me)).toList();
    }

    @Transactional(readOnly = true)
    public List<ConnectionDto> connections(Long me) {
        return repo.connections(me, RequestStatus.ACCEPTED).stream().map(r -> toDto(r, me)).toList();
    }

    @Transactional(readOnly = true)
    public SummaryDto summary(Long me) {
        return new SummaryDto(repo.countByReceiverIdAndStatus(me, RequestStatus.PENDING),
                repo.countBySenderIdAndStatus(me, RequestStatus.PENDING),
                repo.countConnections(me, RequestStatus.ACCEPTED),
                notificationRepo.countByUserIdAndReadFlagFalse(me));
    }

    // ------------------------------------------------------------- helpers
    private ConnectionRequest find(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("Request not found"));
    }

    private ConnectionDto toDto(ConnectionRequest r, Long me) {
        boolean outgoing = r.getSender().getId().equals(me);
        User other = outgoing ? r.getReceiver() : r.getSender();
        Profile otherProfile = profiles.findByUserId(other.getId()).orElseThrow();
        Profile myProfile = profiles.findByUserId(me).orElseThrow();
        // Number is revealed by the mapper ONLY when r.status == ACCEPTED
        return new ConnectionDto(r.getId(), r.getStatus().name(), outgoing ? "OUTGOING" : "INCOMING", r.getMessage(),
                r.getCreatedAt(), r.getRespondedAt(), mapper.toDto(otherProfile, myProfile, r));
    }
}

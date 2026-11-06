package com.campusconnect.service;

import com.campusconnect.dto.Dtos.NotificationDto;
import com.campusconnect.entity.Notification;
import com.campusconnect.entity.User;
import com.campusconnect.exception.ApiException;
import com.campusconnect.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repo;

    @Transactional
    public void create(User user, String type, String title, String body, String link) {
        Notification n = new Notification();
        n.setUser(user); n.setType(type); n.setTitle(title); n.setBody(body); n.setLink(link);
        repo.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(Long userId) {
        return repo.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(),
                        n.isReadFlag(), n.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public long unread(Long userId) { return repo.countByUserIdAndReadFlagFalse(userId); }

    @Transactional
    public void markAllRead(Long userId) { repo.markAllRead(userId); }

    @Transactional
    public void markRead(Long userId, Long id) {
        Notification n = repo.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUser().getId().equals(userId)) throw ApiException.forbidden("Not your notification");
        n.setReadFlag(true);
    }
}

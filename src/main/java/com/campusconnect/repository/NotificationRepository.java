package com.campusconnect.repository;

import com.campusconnect.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadFlagFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.readFlag = true where n.user.id = :uid and n.readFlag = false")
    int markAllRead(@Param("uid") Long uid);
}

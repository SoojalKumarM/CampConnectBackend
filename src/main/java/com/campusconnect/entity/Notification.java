package com.campusconnect.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notif_user", columnList = "user_id"))
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    private User user;

    private String type;
    private String title;
    @Column(length = 300) private String body;
    private String link;

    @Column(name = "is_read")        // READ is a reserved word in MySQL
    private boolean readFlag;

    private Instant createdAt = Instant.now();
}

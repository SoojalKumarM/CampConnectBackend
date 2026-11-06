package com.campusconnect.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String passwordHash;                 // null for Google-only accounts

    @Column(unique = true)
    private String googleId;

    /** Private. Only ever exposed to a user after a connection request between them is ACCEPTED. */
    @Column(unique = true, length = 20)
    private String whatsappNumber;

    private boolean verifiedStudent;
    private boolean profileComplete;
    private boolean active = true;
    private Instant createdAt = Instant.now();
}

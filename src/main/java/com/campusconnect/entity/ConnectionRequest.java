package com.campusconnect.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "connection_requests", indexes = {
        @Index(name = "idx_cr_sender", columnList = "sender_id"),
        @Index(name = "idx_cr_receiver", columnList = "receiver_id")})
@Getter @Setter @NoArgsConstructor
public class ConnectionRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(optional = false) @JoinColumn(name = "receiver_id")
    private User receiver;

    @Column(length = 200)
    private String message;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    private Instant createdAt = Instant.now();
    private Instant respondedAt;
}

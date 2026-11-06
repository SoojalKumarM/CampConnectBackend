package com.campusconnect.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "reports")
@Getter @Setter @NoArgsConstructor
public class Report {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "reporter_id") private User reporter;
    @ManyToOne(optional = false) @JoinColumn(name = "reported_id") private User reported;
    private String reason;
    @Column(length = 1000) private String details;
    private Instant createdAt = Instant.now();
}

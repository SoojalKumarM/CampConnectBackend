package com.campusconnect.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "profiles")
@Getter @Setter @NoArgsConstructor
public class Profile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private String fullName;
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING) private Gender gender;
    @Enumerated(EnumType.STRING) private LookingFor lookingFor;

    private String university;
    private String department;
    private Integer yearOfStudy;

    @Column(length = 500)
    private String bio;

    private boolean visible = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_interests", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "interest", length = 40)
    private Set<String> interests = new LinkedHashSet<>();

    /** Relative paths like /uploads/abc.jpg ; first one is the main photo. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_photos", joinColumns = @JoinColumn(name = "profile_id"))
    @OrderColumn(name = "position")
    @Column(name = "url", length = 200)
    private List<String> photos = new ArrayList<>();
}

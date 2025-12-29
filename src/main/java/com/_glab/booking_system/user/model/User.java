package com._glab.booking_system.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;

@EnableJpaAuditing
@Entity
@Table(name = "account")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(unique = true)
    private String username;

    private String password;

    private String email;

    private String firstName;

    private String lastName;

    @Enumerated(EnumType.ORDINAL)
    private Degree degree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime lastLogin;

    @Column(length = 45)
    private String lastLoginIp;

    private Boolean enabled = true;

    private Integer failedLoginCount = 0;

    private OffsetDateTime lockedUntil;

    private Boolean mfaEnabled = false;

    @Column(length = 128)
    private String totpSecret;

    /**
     * When MFA was enforced for this user (null if not enforced yet).
     * Used to track when admins/lab managers completed required MFA setup.
     */
    private OffsetDateTime mfaEnforcedAt;

    /**
     * JSON array of BCrypt-hashed backup codes for MFA recovery.
     * Example: ["$2a$10$...", "$2a$10$..."]
     */
    @Column(columnDefinition = "TEXT")
    private String backupCodes;

    private OffsetDateTime passwordChangedAt;

    @CreatedDate
    @Column(name = "createdTimestamp", updatable = false)
    private OffsetDateTime createdDate;

    @LastModifiedDate
    @Column(name = "lastModifiedTimestamp")
    private OffsetDateTime lastModifiedDate;

}

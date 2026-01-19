package com._glab.booking_system.booking.model;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workstation", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"lab_id", "identifier"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Workstation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_id", nullable = false)
    private Lab lab;

    /**
     * Unique identifier within the lab (e.g., "WS-01", "A1", "1")
     */
    @Column(nullable = false, length = 20)
    private String identifier;

    private String description;

    /**
     * If false, workstation is disabled and cannot be booked.
     */
    @Column(nullable = false)
    private Boolean active = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_modified_at")
    private OffsetDateTime lastModifiedAt;
}

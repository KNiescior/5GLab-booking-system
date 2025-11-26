package com._glab.booking_system.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;

@Entity
@Table(name = "role")
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;


    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false, length = 64)
    private RoleName name;

    @Column(length = 255)
    private String description;

    @CreatedDate
    @Column(name = "createdTimestamp", updatable = false)
    private OffsetDateTime createdDate;

    @LastModifiedDate
    @Column(name = "lastModifiedTimestamp")
    private OffsetDateTime lastModifiedDate;
}

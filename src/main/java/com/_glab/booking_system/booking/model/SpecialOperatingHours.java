package com._glab.booking_system.booking.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Override operating hours for a specific date (lab or building level).
 */
@Entity
@Table(name = "special_operating_hours", indexes = {
    @Index(name = "idx_special_operating_lab_date", columnList = "lab_id, specific_date"),
    @Index(name = "idx_special_operating_building_date", columnList = "building_id, specific_date")
})
@Getter
@Setter
@NoArgsConstructor
public class SpecialOperatingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    /**
     * If set, override applies to this lab only.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_id")
    private Lab lab;

    /**
     * If set (and lab null), override applies to this building only.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @Column(name = "specific_date", nullable = false)
    private LocalDate specificDate;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "is_closed")
    private Boolean isClosed = false;
}

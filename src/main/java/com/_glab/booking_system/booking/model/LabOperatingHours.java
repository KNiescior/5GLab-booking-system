package com._glab.booking_system.booking.model;

import java.time.LocalTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lab_operating_hours", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"lab_id", "day_of_week"})
})
@Getter
@Setter
@NoArgsConstructor
public class LabOperatingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_id", nullable = false)
    private Lab lab;

    /**
     * Day of week: 0 = Sunday, 1 = Monday, ..., 6 = Saturday
     */
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    /**
     * If true, the lab is closed on this day regardless of open/close times.
     */
    @Column(name = "is_closed")
    private Boolean isClosed = false;
}

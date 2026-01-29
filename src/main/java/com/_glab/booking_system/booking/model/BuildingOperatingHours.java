package com._glab.booking_system.booking.model;

import java.time.LocalTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "building_operating_hours", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"building_id", "day_of_week"})
})
@Getter
@Setter
@NoArgsConstructor
public class BuildingOperatingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

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
     * If true, the building is closed on this day regardless of open/close times.
     */
    @Column(name = "is_closed")
    private Boolean isClosed = false;
}

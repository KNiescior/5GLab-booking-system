package com._glab.booking_system.booking.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "building_closed_day", indexes = {
    @Index(name = "idx_building_closed_day_building", columnList = "building_id"),
    @Index(name = "idx_building_closed_day_date", columnList = "specific_date")
})
@Getter
@Setter
@NoArgsConstructor
public class BuildingClosedDay {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "specific_date")
    private LocalDate specificDate;

    @Column(name = "recurring_day_of_week")
    private Integer recurringDayOfWeek;

    private String reason;
}

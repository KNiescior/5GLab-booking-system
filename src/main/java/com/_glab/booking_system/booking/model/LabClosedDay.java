package com._glab.booking_system.booking.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lab_closed_day", indexes = {
    @Index(name = "idx_lab_closed_day_lab", columnList = "lab_id"),
    @Index(name = "idx_lab_closed_day_date", columnList = "specific_date")
})
@Getter
@Setter
@NoArgsConstructor
public class LabClosedDay {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    /**
     * If null, applies to all labs (global closure).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_id")
    private Lab lab;

    /**
     * Specific date when lab is closed (e.g., holidays, maintenance).
     * Either specificDate or recurringDayOfWeek should be set.
     */
    @Column(name = "specific_date")
    private LocalDate specificDate;

    /**
     * Recurring day of week: 0 = Sunday, 1 = Monday, ..., 6 = Saturday.
     * Used for recurring closures (e.g., every Sunday).
     * Either specificDate or recurringDayOfWeek should be set.
     */
    @Column(name = "recurring_day_of_week")
    private Integer recurringDayOfWeek;

    /**
     * Reason for closure (e.g., "National Holiday", "Maintenance", "Sunday")
     */
    private String reason;
}

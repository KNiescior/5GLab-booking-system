package com._glab.booking_system.booking.model;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recurring_pattern", indexes = {
    @Index(name = "idx_recurring_pattern_group", columnList = "recurring_group_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class RecurringPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Links to the recurring_group_id in Reservation table.
     * One pattern per recurring group.
     */
    @Column(name = "recurring_group_id", nullable = false, unique = true)
    private UUID recurringGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pattern_type", nullable = false, length = 20)
    private RecurrenceType patternType;

    /**
     * Number of days between occurrences (for CUSTOM pattern).
     * For WEEKLY=7, BIWEEKLY=14, MONTHLY=calculated.
     */
    @Column(name = "interval_days")
    private Integer intervalDays;

    /**
     * End date for the recurring series (exclusive).
     * Either endDate or occurrences should be set, not both.
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Number of occurrences in the series.
     * Either endDate or occurrences should be set, not both.
     */
    private Integer occurrences;
}
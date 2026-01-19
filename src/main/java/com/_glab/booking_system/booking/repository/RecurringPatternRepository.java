package com._glab.booking_system.booking.repository;

import com._glab.booking_system.booking.model.RecurringPattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RecurringPatternRepository extends JpaRepository<RecurringPattern, UUID> {

    Optional<RecurringPattern> findByRecurringGroupId(UUID recurringGroupId);
}

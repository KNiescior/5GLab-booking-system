package com._glab.booking_system.booking.repository;

import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.Workstation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkstationRepository extends JpaRepository<Workstation, Integer> {

    List<Workstation> findByLab(Lab lab);

    List<Workstation> findByLabId(Integer labId);

    List<Workstation> findByLabIdAndActiveTrue(Integer labId);

    Optional<Workstation> findByLabAndIdentifier(Lab lab, String identifier);

    int countByLabIdAndActiveTrue(Integer labId);
}

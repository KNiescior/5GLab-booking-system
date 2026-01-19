package com._glab.booking_system.booking.repository;

import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.LabManager;
import com._glab.booking_system.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabManagerRepository extends JpaRepository<LabManager, Integer> {

    List<LabManager> findByLab(Lab lab);

    List<LabManager> findByLabId(Integer labId);

    List<LabManager> findByUser(User user);

    List<LabManager> findByUserId(Integer userId);

    Optional<LabManager> findByLabAndUser(Lab lab, User user);

    Optional<LabManager> findByLabIdAndIsPrimaryTrue(Integer labId);

    boolean existsByLabAndUser(Lab lab, User user);
}

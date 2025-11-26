package com._glab.booking_system.user.repository;

import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
	Optional<Role> findByName(RoleName name);
}



package com._glab.booking_system.user.repository;

import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RoleRepositoryTests {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private RoleRepository roleRepository;

	@Test
	void findByName_returnsSavedRole() {
		Role admin = new Role();
		admin.setName(RoleName.ADMIN);
		admin.setDescription("Administrator");
		em.persist(admin);
		em.flush();

		Optional<Role> found = roleRepository.findByName(RoleName.ADMIN);
		assertThat(found).isPresent();
		assertThat(found.get().getName()).isEqualTo(RoleName.ADMIN);
	}
}



package com._glab.booking_system.user.repository;

import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTests {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private UserRepository userRepository;

	@Test
	void findByEmail_and_findByUsername_work() {
		Role role = new Role();
		role.setName(RoleName.USER);
		em.persist(role);

		User u = new User();
		u.setEmail("alice@example.edu");
		u.setUsername("alice");
		u.setPassword("{noop}secret");
		u.setRole(role);
		em.persist(u);
		em.flush();

		assertThat(userRepository.findByEmail("alice@example.edu")).isPresent();
		assertThat(userRepository.findByUsername("alice")).isPresent();
	}
}



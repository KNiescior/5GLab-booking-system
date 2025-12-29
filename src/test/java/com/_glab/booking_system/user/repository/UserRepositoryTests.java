package com._glab.booking_system.user.repository;

import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class UserRepositoryTests {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@DynamicPropertySource
	static void registerProps(DynamicPropertyRegistry r) {
		if (postgres.isRunning()) {
			r.add("spring.datasource.url", postgres::getJdbcUrl);
			r.add("spring.datasource.username", postgres::getUsername);
			r.add("spring.datasource.password", postgres::getPassword);
			r.add("spring.jpa.hibernate.ddl-auto", () -> "update");
		}
	}

	@Autowired
	private TestEntityManager em;

	@Autowired
	private UserRepository userRepository;

	@Test
	void findByEmail_and_findByUsername_work() {
		Role role = new Role();
		role.setName(RoleName.PROFESSOR);
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



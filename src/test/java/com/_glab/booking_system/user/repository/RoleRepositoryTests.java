package com._glab.booking_system.user.repository;

import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class RoleRepositoryTests {

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



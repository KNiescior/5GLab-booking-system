package com._glab.booking_system.auth.service;

import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Spring Security entry point for loading a user.
     * We treat the \"username\" parameter as the user's email.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for email: " + email));

        boolean enabled = Boolean.TRUE.equals(user.getEnabled());
        boolean accountNonLocked = user.getLockedUntil() == null
                || user.getLockedUntil().isBefore(OffsetDateTime.now());
        boolean accountNonExpired = true;
        boolean credentialsNonExpired = true;

        Role role = user.getRole();
        String roleName = role != null ? role.getName().name() : "USER";

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(roleName)
                .accountLocked(!accountNonLocked)
                .disabled(!enabled)
                .accountExpired(!accountNonExpired)
                .credentialsExpired(!credentialsNonExpired)
                .build();
    }
}



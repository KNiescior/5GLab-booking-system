package com._glab.booking_system.user.response;

import com._glab.booking_system.user.model.Degree;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Response DTO for user information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Integer id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private Degree degree;
    private RoleName role;
    private Boolean enabled;
    private OffsetDateTime createdAt;

    /**
     * Create a UserResponse from a User entity.
     */
    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .degree(user.getDegree())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedDate())
                .build();
    }
}

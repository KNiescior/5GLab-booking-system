package com._glab.booking_system.user.request;

import com._glab.booking_system.user.model.Degree;
import com._glab.booking_system.user.model.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for admin updating a user (profile + role).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUpdateUserRequest {

    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    private Degree degree;

    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String email;

    private RoleName roleName;
}

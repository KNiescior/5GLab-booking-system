package com._glab.booking_system.booking.response;

import com._glab.booking_system.booking.model.LabManager;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for lab manager assignment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabManagerResponse {

    private Integer id;
    private Integer userId;
    private String userEmail;
    private Boolean isPrimary;

    public static LabManagerResponse fromLabManager(LabManager lm) {
        return LabManagerResponse.builder()
                .id(lm.getId())
                .userId(lm.getUser() != null ? lm.getUser().getId() : null)
                .userEmail(lm.getUser() != null ? lm.getUser().getEmail() : null)
                .isPrimary(lm.getIsPrimary())
                .build();
    }
}

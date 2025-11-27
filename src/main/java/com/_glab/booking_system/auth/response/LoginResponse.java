package com._glab.booking_system.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private LoggedInUser user;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class LoggedInUser {
        private Integer id;
        private String email;
        private String role;
    }
}



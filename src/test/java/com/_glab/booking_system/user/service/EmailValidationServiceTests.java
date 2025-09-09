package com._glab.booking_system.user.service;

import com._glab.booking_system.user.exception.InvalidEmailException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class EmailValidationServiceTests {

    @Autowired
    private EmailValidationService emailValidationService;

    public static Stream<Arguments> provideEmailAdressList() {
        return Stream.of(
                Arguments.of("tukipi_zavi74@yahoo.com", true),
                Arguments.of("doj-ijibego57@outlook.com", true),
                Arguments.of("pogoket_ini37@mail.com", true),
                Arguments.of("vumoho_wasu81@yahoo.com", true),
                Arguments.of("goza_kopixa80@gmail.com", true),
                Arguments.of("yez_opabapa48@aol.com", true),
                Arguments.of("zey_iyisaxi39@outlook.com", true),
                Arguments.of("tibel_afixa20@gmail.com", true),
                Arguments.of("gubuzav_afe6@yahoo.com", true),
                Arguments.of("gubuzav_afe6", false),
                Arguments.of("", false),
                Arguments.of("gubuśzav_afe6@yahoo.com", true),
                Arguments.of("gubuzav_afe6@@yahoo.com", false)
        );
    }

    @ParameterizedTest
    @MethodSource("provideEmailAdressList")
    void contextLoads(String email, boolean isValid) {
        if (isValid) {
            assertDoesNotThrow(() -> emailValidationService.validateEmail(email));
        } else {
            assertThrows(InvalidEmailException.class, () -> emailValidationService.validateEmail(email));
        }
    }
}

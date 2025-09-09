package com._glab.booking_system.user.service;

import com._glab.booking_system.user.exception.InvalidEmailException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.stereotype.Service;

@Service
public class EmailValidationService {

    public void validateEmail(String email) {
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException e) {
            throw new InvalidEmailException("Invalid email address: " + email);
        }
    }
}
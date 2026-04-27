package com.toanlv.flashsale.auth.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpGenerator {

    private final SecureRandom random = new SecureRandom();
    private final int length;

    public OtpGenerator(@Value("${app.otp.length:6}") int length) {
        this.length = length;
    }

    /**
     * Generate a zero-padded numeric OTP of configured length.
     * Uses SecureRandom — cryptographically strong, not predictable.
     *
     * @return e.g. "034521" for length=6
     */
    public String generate() {
        int bound = (int) Math.pow(10, length);
        int n     = random.nextInt(bound);
        return String.format("%0" + length + "d", n);
    }
}

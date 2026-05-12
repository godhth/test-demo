package com.webox.webox.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class PasswordEncoder {
    private final SecureRandom random = new SecureRandom();

    public String newSalt() {
        byte[] b = new byte[16];
        random.nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }

    public String hash(String password, String saltB64) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(saltB64));
            return HexFormat.of().formatHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean matches(String password, String saltB64, String expectedHex) {
        return MessageDigest.isEqual(
                hash(password, saltB64).getBytes(StandardCharsets.US_ASCII),
                expectedHex.getBytes(StandardCharsets.US_ASCII));
    }
}

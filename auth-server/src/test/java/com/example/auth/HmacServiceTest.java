package com.example.auth;

import com.example.auth.security.HmacService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles; // AJOUTÉ

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class HmacServiceTest {

    @Autowired
    private HmacService hmacService;

    @Test
    void testComparaisonTempsConstantSignaturesEgales() {
        String sig = hmacService.compute("monSecret", "email:nonce:123456");
        assertTrue(hmacService.verifyConstantTime(sig, sig));
    }

    @Test
    void testComparaisonTempsConstantSignaturesDifferentes() {
        String sig1 = hmacService.compute("monSecret", "email:nonce:123456");
        String sig2 = hmacService.compute("autreSecret", "email:nonce:123456");
        assertFalse(hmacService.verifyConstantTime(sig1, sig2));
    }

    @Test
    void testComputeProduireResultatDifferentAvecMessagesDistincts() {
        String sig1 = hmacService.compute("secret", "message1");
        String sig2 = hmacService.compute("secret", "message2");
        assertNotEquals(sig1, sig2);
    }
}
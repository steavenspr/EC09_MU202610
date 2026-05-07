package com.example.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles; // AJOUTÉ

@SpringBootTest
@ActiveProfiles("test") // AJOUTÉ
class AuthApplicationTests {

	@Test
	void contextLoads() {
	}

}
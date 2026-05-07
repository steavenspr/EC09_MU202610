package com.example.auth;

import com.example.auth.security.HmacService;
import com.example.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles; // AJOUTÉ
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // INDISPENSABLE POUR GITHUB ACTIONS
@Transactional
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private HmacService hmacService;

    private static final String VALID_PASSWORD = "Motdepasse1!";

    private String buildLoginJson(String email, String nonce, long timestamp, String hmac) {
        return String.format(
                "{\"email\":\"%s\",\"nonce\":\"%s\",\"timestamp\":%d,\"hmac\":\"%s\"}",
                email, nonce, timestamp, hmac
        );
    }

    @Test
    void testRegisterEmailInvalideRetourne400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .param("email", "emailinvalide")
                        .param("password", VALID_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testRegisterEmailDejaExistantRetourne409() throws Exception {
        authService.register("conflit@example.com", VALID_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .param("email", "conflit@example.com")
                        .param("password", VALID_PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testLoginEmailInconnuRetourne401() throws Exception {
        String email = "inconnu@example.com";
        String nonce = UUID.randomUUID().toString();
        long timestamp = Instant.now().getEpochSecond();
        String hmac = hmacService.compute(VALID_PASSWORD, email + ":" + nonce + ":" + timestamp);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildLoginJson(email, nonce, timestamp, hmac)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testLoginCompteBloqueRetourne423() throws Exception {
        String email = "bloque@example.com";
        authService.register(email, VALID_PASSWORD);

        // Simulation de 5 échecs pour déclencher le lockout
        for (int i = 0; i < 5; i++) {
            String nonce = UUID.randomUUID().toString();
            long timestamp = Instant.now().getEpochSecond();
            String badHmac = hmacService.compute("MauvaisMotDePasse1!", email + ":" + nonce + ":" + timestamp);
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(buildLoginJson(email, nonce, timestamp, badHmac)));
        }

        String nonce = UUID.randomUUID().toString();
        long timestamp = Instant.now().getEpochSecond();
        String hmac = hmacService.compute(VALID_PASSWORD, email + ":" + nonce + ":" + timestamp);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildLoginJson(email, nonce, timestamp, hmac)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.status").value(423));
    }

    @Test
    void testMeAvecTokenValideRetourne200() throws Exception {
        String email = "meok@example.com";
        authService.register(email, VALID_PASSWORD);
        String nonce = UUID.randomUUID().toString();
        long timestamp = Instant.now().getEpochSecond();
        String hmac = hmacService.compute(VALID_PASSWORD, email + ":" + nonce + ":" + timestamp);
        String token = authService.login(email, nonce, timestamp, hmac);

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("apprenant"));
    }

    @Test
    void testMeTokenInvalideRetourne401() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer tokeninvalide"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testMeSansHeaderRetourne401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }
}
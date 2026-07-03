package com.trust.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_succeeds_withSeededOwnerCredentials() throws Exception {
        login("owner@trust.demo", "password123")
                .andExpect(status().isOk());
    }

    @Test
    void login_fails_withWrongPassword() throws Exception {
        login("owner@trust.demo", "not-the-password")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_locksOutAfterFiveFailedAttempts() throws Exception {
        // بريد مخصص لهذا الاختبار وحده حتى لا يؤثر على محاولات owner@trust.demo في اختبارات أخرى
        // تشارك نفس سياق Spring (LoginAttemptService في الذاكرة يُحمَّل مرة واحدة لكل سياق)
        String email = "lockout-test@trust.demo";
        for (int i = 0; i < 5; i++) {
            login(email, "wrong").andExpect(status().isUnauthorized());
        }
        login(email, "wrong").andExpect(status().isTooManyRequests());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }
}

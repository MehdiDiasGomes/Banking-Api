package com.mehdi.banking_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehdi.banking_api.dto.request.RegisterRequest;
import com.mehdi.banking_api.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private Cookie jwtCookie;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("me-user@test.com");
        req.setPassword("password123");
        req.setFirstName("John");
        req.setLastName("Doe");

        MockHttpServletResponse response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse();

        jwtCookie = new Cookie("jwt", response.getCookie("jwt").getValue());
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void me_withoutAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void me_withAuth_returns200AndUserProfile() throws Exception {
        mockMvc.perform(get("/api/users/me").cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me-user@test.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.id").isString());
    }
}

package com.mehdi.banking_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehdi.banking_api.dto.request.LoginRequest;
import com.mehdi.banking_api.dto.request.RegisterRequest;
import com.mehdi.banking_api.repository.BeneficiaryRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BeneficiaryControllerIntegrationTest {

    // Valid LU IBAN that is NOT in the H2 database (external IBAN)
    private static final String EXTERNAL_IBAN = "LU280019400644750000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private UserRepository userRepository;

    private Cookie jwtCookie;

    @BeforeEach
    void setUp() throws Exception {
        jwtCookie = registerAndGetCookie("ben-user@test.com", "John", "Doe");
    }

    @AfterEach
    void tearDown() {
        beneficiaryRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Cookie registerAndGetCookie(String email, String firstName, String lastName) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword("password123");
        req.setFirstName(firstName);
        req.setLastName(lastName);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        userRepository.findByEmail(email).ifPresent(u -> {
            u.setVerified(true);
            userRepository.save(u);
        });

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(email);
        loginReq.setPassword("password123");

        MockHttpServletResponse response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn().getResponse();

        return new Cookie("jwt", response.getCookie("jwt").getValue());
    }

    // ── GET /api/beneficiaries ────────────────────────────────────────────────

    @Test
    void getAll_withoutAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/beneficiaries"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_withAuth_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/beneficiaries").cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── POST /api/beneficiaries/validate ─────────────────────────────────────

    @Test
    void validate_withoutAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/beneficiaries/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iban\":\"" + EXTERNAL_IBAN + "\",\"firstName\":\"John\",\"lastName\":\"Doe\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validate_withExternalIban_returns200AndNameMatchTrue() throws Exception {
        mockMvc.perform(post("/api/beneficiaries/validate")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iban\":\"" + EXTERNAL_IBAN + "\",\"firstName\":\"Anyone\",\"lastName\":\"Whatever\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameMatch").value(true))
                .andExpect(jsonPath("$.countryCode").value("LU"))
                .andExpect(jsonPath("$.bankCode").isString());
    }

    @Test
    void validate_withInvalidIban_returns400() throws Exception {
        mockMvc.perform(post("/api/beneficiaries/validate")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iban\":\"NOT-AN-IBAN\",\"firstName\":\"John\",\"lastName\":\"Doe\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validate_withMissingField_returns400() throws Exception {
        mockMvc.perform(post("/api/beneficiaries/validate")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iban\":\"" + EXTERNAL_IBAN + "\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/beneficiaries ───────────────────────────────────────────────

    @Test
    void create_withoutAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iban\":\"" + EXTERNAL_IBAN + "\",\"firstName\":\"Alice\",\"lastName\":\"Martin\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withValidRequest_returns201AndBeneficiary() throws Exception {
        mockMvc.perform(post("/api/beneficiaries")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iban\":\"" + EXTERNAL_IBAN + "\",\"firstName\":\"Alice\",\"lastName\":\"Martin\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.iban").isString())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Martin"))
                .andExpect(jsonPath("$.countryCode").value("LU"));
    }

    @Test
    void create_beneficiaryAppearsInGetAll() throws Exception {
        mockMvc.perform(post("/api/beneficiaries")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iban\":\"" + EXTERNAL_IBAN + "\",\"firstName\":\"Alice\",\"lastName\":\"Martin\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/beneficiaries").cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Alice"));
    }

    @Test
    void create_beneficiaryNotVisibleToOtherUser() throws Exception {
        Cookie otherCookie = registerAndGetCookie("other-ben@test.com", "Jane", "Smith");

        mockMvc.perform(post("/api/beneficiaries")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iban\":\"" + EXTERNAL_IBAN + "\",\"firstName\":\"Alice\",\"lastName\":\"Martin\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/beneficiaries").cookie(otherCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── DELETE /api/beneficiaries/{id} ────────────────────────────────────────

    @Test
    void delete_withoutAuth_returns403() throws Exception {
        mockMvc.perform(delete("/api/beneficiaries/" + java.util.UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withOwner_returns204AndRemovesBeneficiary() throws Exception {
        String id = objectMapper.readTree(
                mockMvc.perform(post("/api/beneficiaries")
                                .cookie(jwtCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"iban\":\"" + EXTERNAL_IBAN + "\",\"firstName\":\"Alice\",\"lastName\":\"Martin\"}"))
                        .andReturn().getResponse().getContentAsString()
        ).get("id").asText();

        mockMvc.perform(delete("/api/beneficiaries/" + id).cookie(jwtCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/beneficiaries").cookie(jwtCookie))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void delete_withNonOwner_returns403() throws Exception {
        Cookie otherCookie = registerAndGetCookie("delete-other@test.com", "Jane", "Smith");

        String id = objectMapper.readTree(
                mockMvc.perform(post("/api/beneficiaries")
                                .cookie(jwtCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"iban\":\"" + EXTERNAL_IBAN + "\",\"firstName\":\"Alice\",\"lastName\":\"Martin\"}"))
                        .andReturn().getResponse().getContentAsString()
        ).get("id").asText();

        mockMvc.perform(delete("/api/beneficiaries/" + id).cookie(otherCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withUnknownId_returns404() throws Exception {
        mockMvc.perform(delete("/api/beneficiaries/" + java.util.UUID.randomUUID()).cookie(jwtCookie))
                .andExpect(status().isNotFound());
    }
}

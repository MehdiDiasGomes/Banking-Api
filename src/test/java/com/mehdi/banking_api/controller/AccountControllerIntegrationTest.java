package com.mehdi.banking_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehdi.banking_api.dto.request.CreateAccountRequest;
import com.mehdi.banking_api.dto.request.RegisterRequest;
import com.mehdi.banking_api.model.AccountType;
import com.mehdi.banking_api.repository.AccountRepository;
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
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private Cookie jwtCookie;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("account-user@test.com");
        req.setPassword("password123");
        req.setFirstName("Jane");
        req.setLastName("Doe");

        MockHttpServletResponse response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse();

        jwtCookie = new Cookie("jwt", response.getCookie("jwt").getValue());
    }

    @AfterEach
    void tearDown() {
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Cookie registerAndGetCookie(String email) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword("password123");
        req.setFirstName("Test");
        req.setLastName("User");

        MockHttpServletResponse response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse();

        return new Cookie("jwt", response.getCookie("jwt").getValue());
    }

    @Test
    void getAll_withoutAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_withAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/accounts").cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void create_withoutAuth_returns403() throws Exception {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(AccountType.SAVINGS);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withAuth_returns201AndAccount() throws Exception {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(AccountType.SAVINGS);

        mockMvc.perform(post("/api/accounts")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.iban").isString())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.type").value("SAVINGS"));
    }

    @Test
    void deposit_withAuth_returns200AndUpdatesBalance() throws Exception {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(AccountType.SAVINGS);

        String iban = objectMapper.readTree(
                mockMvc.perform(post("/api/accounts")
                                .cookie(jwtCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andReturn().getResponse().getContentAsString()
        ).get("iban").asText();

        mockMvc.perform(post("/api/accounts/" + iban + "/deposit")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":250.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(250.0));
    }

    @Test
    void deposit_withForeignAccount_returns403() throws Exception {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(AccountType.SAVINGS);

        String iban = objectMapper.readTree(
                mockMvc.perform(post("/api/accounts")
                                .cookie(jwtCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andReturn().getResponse().getContentAsString()
        ).get("iban").asText();

        Cookie otherCookie = registerAndGetCookie("other-deposit@test.com");

        mockMvc.perform(post("/api/accounts/" + iban + "/deposit")
                        .cookie(otherCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_accountBelongsToAuthenticatedUser() throws Exception {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(AccountType.CHECKING);

        mockMvc.perform(post("/api/accounts")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/accounts").cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CHECKING"));
    }
}

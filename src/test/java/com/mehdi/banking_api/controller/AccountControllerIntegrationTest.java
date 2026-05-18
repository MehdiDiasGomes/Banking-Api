package com.mehdi.banking_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehdi.banking_api.dto.request.CreateAccountRequest;
import com.mehdi.banking_api.dto.request.DeleteAccountRequest;
import com.mehdi.banking_api.dto.request.RegisterRequest;
import com.mehdi.banking_api.model.AccountType;
import com.mehdi.banking_api.repository.AccountRepository;
import com.mehdi.banking_api.repository.TransactionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    private TransactionRepository transactionRepository;

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
        transactionRepository.deleteAll();
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

    // ── DELETE /api/accounts ──────────────────────────────────────────────────

    @Test
    void delete_withoutAuth_returns403() throws Exception {
        mockMvc.perform(delete("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iban\":\"LU001\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_zeroBalanceAccount_returns204() throws Exception {
        String iban = createAccount(jwtCookie, AccountType.CHECKING);

        DeleteAccountRequest req = new DeleteAccountRequest(iban, null);

        mockMvc.perform(delete("/api/accounts")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        assertThat(accountRepository.findAll()).isEmpty();
    }

    @Test
    void delete_withBalance_andValidDestination_returns204AndTransfersBalance() throws Exception {
        String sourceIban = createAccount(jwtCookie, AccountType.CHECKING);
        String destIban   = createAccount(jwtCookie, AccountType.SAVINGS);

        // Deposit 300 into source
        mockMvc.perform(post("/api/accounts/" + sourceIban + "/deposit")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":300}"))
                .andExpect(status().isOk());

        DeleteAccountRequest req = new DeleteAccountRequest(sourceIban, destIban);

        mockMvc.perform(delete("/api/accounts")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        // Source account gone, destination balance updated
        assertThat(accountRepository.findAll()).hasSize(1);
        assertThat(accountRepository.findAll().get(0).getIban()).isEqualTo(destIban);
        assertThat(accountRepository.findAll().get(0).getBalance())
                .isEqualByComparingTo("300.0000");
    }

    @Test
    void delete_withBalance_noDestination_returns400() throws Exception {
        String iban = createAccount(jwtCookie, AccountType.CHECKING);

        mockMvc.perform(post("/api/accounts/" + iban + "/deposit")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100}"))
                .andExpect(status().isOk());

        DeleteAccountRequest req = new DeleteAccountRequest(iban, null);

        mockMvc.perform(delete("/api/accounts")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_foreignAccount_returns403() throws Exception {
        String iban = createAccount(jwtCookie, AccountType.CHECKING);

        Cookie otherCookie = registerAndGetCookie("other-delete@test.com");

        DeleteAccountRequest req = new DeleteAccountRequest(iban, null);

        mockMvc.perform(delete("/api/accounts")
                        .cookie(otherCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_destinationBelongsToAnotherUser_returns403() throws Exception {
        String sourceIban = createAccount(jwtCookie, AccountType.CHECKING);

        mockMvc.perform(post("/api/accounts/" + sourceIban + "/deposit")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100}"))
                .andExpect(status().isOk());

        Cookie otherCookie = registerAndGetCookie("dest-owner@test.com");
        String otherIban = createAccount(otherCookie, AccountType.SAVINGS);

        DeleteAccountRequest req = new DeleteAccountRequest(sourceIban, otherIban);

        mockMvc.perform(delete("/api/accounts")
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    /** Creates an account and returns its IBAN. */
    private String createAccount(Cookie cookie, AccountType type) throws Exception {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(type);

        return objectMapper.readTree(
                mockMvc.perform(post("/api/accounts")
                                .cookie(cookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andReturn().getResponse().getContentAsString()
        ).get("iban").asText();
    }
}

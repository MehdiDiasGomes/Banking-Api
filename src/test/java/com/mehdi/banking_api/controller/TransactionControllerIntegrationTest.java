package com.mehdi.banking_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehdi.banking_api.dto.request.CreateAccountRequest;
import com.mehdi.banking_api.dto.request.RegisterRequest;
import com.mehdi.banking_api.dto.request.TransferRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private Cookie aliceCookie;
    private Cookie bobCookie;
    private String aliceIban;
    private String bobIban;

    @BeforeEach
    void setUp() throws Exception {
        aliceCookie = registerAndGetCookie("alice-tx@test.com");
        bobCookie = registerAndGetCookie("bob-tx@test.com");
        aliceIban = createAccountAndGetIban(aliceCookie);
        bobIban = createAccountAndGetIban(bobCookie);
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
        req.setFirstName("User");
        req.setLastName("Test");

        MockHttpServletResponse response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse();

        return new Cookie("jwt", response.getCookie("jwt").getValue());
    }

    private void depositFunds(String iban, Cookie cookie, double amount) throws Exception {
        mockMvc.perform(post("/api/accounts/" + iban + "/deposit")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount + "}"))
                .andExpect(status().isOk());
    }

    private String createAccountAndGetIban(Cookie cookie) throws Exception {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(AccountType.SAVINGS);

        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("iban").asText();
    }

    @Test
    void getHistory_withoutAuth_returns403() throws Exception {
        mockMvc.perform(get("/transactions/history").param("iban", aliceIban))
                .andExpect(status().isForbidden());
    }

    @Test
    void getHistory_withOwnIban_returns200() throws Exception {
        mockMvc.perform(get("/transactions/history")
                        .cookie(aliceCookie)
                        .param("iban", aliceIban))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getHistory_withForeignIban_returns400() throws Exception {
        mockMvc.perform(get("/transactions/history")
                        .cookie(aliceCookie)
                        .param("iban", bobIban))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to view this account's history"));
    }

    @Test
    void getHistory_withUnknownIban_returns404() throws Exception {
        mockMvc.perform(get("/transactions/history")
                        .cookie(aliceCookie)
                        .param("iban", "LU_UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void transfer_withoutAuth_returns403() throws Exception {
        TransferRequest req = new TransferRequest();
        req.setSenderIban(aliceIban);
        req.setReceiverIban(bobIban);
        req.setAmount(10.0);

        mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void transfer_withUnauthorizedSender_returns400() throws Exception {
        TransferRequest req = new TransferRequest();
        req.setSenderIban(bobIban);
        req.setReceiverIban(aliceIban);
        req.setAmount(10.0);

        mockMvc.perform(post("/transactions/transfer")
                        .cookie(aliceCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to transfer from this account"));
    }

    @Test
    void transfer_withInsufficientBalance_returns400() throws Exception {
        TransferRequest req = new TransferRequest();
        req.setSenderIban(aliceIban);
        req.setReceiverIban(bobIban);
        req.setAmount(9999.0);

        mockMvc.perform(post("/transactions/transfer")
                        .cookie(aliceCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient balance"));
    }

    @Test
    void transfer_withValidRequest_returns200AndAppearsInHistory() throws Exception {
        depositFunds(aliceIban, aliceCookie, 100.0);

        TransferRequest req = new TransferRequest();
        req.setSenderIban(aliceIban);
        req.setReceiverIban(bobIban);
        req.setAmount(50.0);

        mockMvc.perform(post("/transactions/transfer")
                        .cookie(aliceCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.senderIban").value(aliceIban))
                .andExpect(jsonPath("$.data.receiverIban").value(bobIban));

        mockMvc.perform(get("/transactions/history")
                        .cookie(aliceCookie)
                        .param("iban", aliceIban))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }
}

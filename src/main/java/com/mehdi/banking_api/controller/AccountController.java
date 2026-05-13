package com.mehdi.banking_api.controller;

import com.mehdi.banking_api.dto.request.CreateAccountRequest;
import com.mehdi.banking_api.dto.response.AccountResponse;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @GetMapping
    public List<AccountResponse> getAll() {
        return accountService.findAllByUser(getAuthenticatedUser());
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(201).body(accountService.save(getAuthenticatedUser(), request));
    }

    private User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}

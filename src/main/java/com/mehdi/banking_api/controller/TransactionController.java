package com.mehdi.banking_api.controller;


import com.mehdi.banking_api.common.ApiResponse;
import com.mehdi.banking_api.dto.response.TransactionResponse;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getHistory() {
        User user = (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getHistory(user)
        ));
    }
}

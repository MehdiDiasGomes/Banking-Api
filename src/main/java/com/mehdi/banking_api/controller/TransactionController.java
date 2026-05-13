package com.mehdi.banking_api.controller;


import com.mehdi.banking_api.common.ApiResponse;
import com.mehdi.banking_api.dto.request.TransferRequest;
import com.mehdi.banking_api.dto.response.TransactionResponse;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/transactions")
@RequiredArgsConstructor
@RestController
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getHistory() {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getHistory(getAuthenticatedUser())
        ));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(@RequestBody TransferRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.transfer(getAuthenticatedUser(), request)
        ));
    }

    private User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}

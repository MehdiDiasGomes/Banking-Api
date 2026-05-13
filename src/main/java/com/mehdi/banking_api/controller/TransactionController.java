package com.mehdi.banking_api.controller;

import com.mehdi.banking_api.common.ApiResult;
import com.mehdi.banking_api.dto.request.TransferRequest;
import com.mehdi.banking_api.dto.response.TransactionResponse;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Transactions", description = "Transfer funds and consult transaction history")
@SecurityRequirement(name = "jwt-cookie")
@RequestMapping("/transactions")
@RequiredArgsConstructor
@RestController
public class TransactionController {
    private final TransactionService transactionService;

    @Operation(summary = "Transaction history", description = "Returns all transactions for the given IBAN. The account must belong to the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "History returned"),
        @ApiResponse(responseCode = "400", description = "Account does not belong to you"),
        @ApiResponse(responseCode = "404", description = "IBAN not found")
    })
    @GetMapping("/history")
    public ResponseEntity<ApiResult<List<TransactionResponse>>> getHistory(
            @Parameter(description = "IBAN of the account", example = "LU1778664822152")
            @RequestParam String iban) {
        return ResponseEntity.ok(ApiResult.success(
                transactionService.getHistory(iban, getAuthenticatedUser())
        ));
    }

    @Operation(summary = "Transfer funds", description = "Transfer an amount between two accounts by IBAN. The sender account must belong to the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transfer completed"),
        @ApiResponse(responseCode = "400", description = "Insufficient balance or unauthorized sender"),
        @ApiResponse(responseCode = "404", description = "Sender or receiver IBAN not found")
    })
    @PostMapping("/transfer")
    public ResponseEntity<ApiResult<TransactionResponse>> transfer(@RequestBody @Valid TransferRequest request) {
        return ResponseEntity.ok(ApiResult.success(
                transactionService.transfer(getAuthenticatedUser(), request)
        ));
    }

    private User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}

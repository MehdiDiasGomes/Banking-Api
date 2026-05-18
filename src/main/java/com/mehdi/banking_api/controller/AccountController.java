package com.mehdi.banking_api.controller;

import com.mehdi.banking_api.dto.request.CreateAccountRequest;
import com.mehdi.banking_api.dto.request.DeleteAccountRequest;
import com.mehdi.banking_api.dto.request.DepositRequest;
import com.mehdi.banking_api.dto.response.AccountResponse;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Accounts", description = "Manage bank accounts for the authenticated user")
@SecurityRequirement(name = "jwt-cookie")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @Operation(summary = "List accounts", description = "Returns all accounts belonging to the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List returned"),
        @ApiResponse(responseCode = "403", description = "Not authenticated")
    })
    @GetMapping
    public List<AccountResponse> getAll() {
        return accountService.findAllByUser(getAuthenticatedUser());
    }

    @Operation(summary = "Create account", description = "Creates a new bank account for the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created"),
        @ApiResponse(responseCode = "403", description = "Not authenticated")
    })
    @PostMapping
    public ResponseEntity<AccountResponse> create(@RequestBody @Valid CreateAccountRequest request) {
        return ResponseEntity.status(201).body(accountService.save(getAuthenticatedUser(), request));
    }

    @Operation(summary = "Deposit funds", description = "Deposits an amount into the specified account owned by the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Deposit successful"),
        @ApiResponse(responseCode = "403", description = "Account does not belong to authenticated user"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PostMapping("/{iban}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable String iban,
            @RequestBody @Valid DepositRequest request) {
        return ResponseEntity.ok(accountService.deposit(iban, request.getAmount(), getAuthenticatedUser()));
    }

    @Operation(summary = "Delete account", description = "Closes the account identified by IBAN. If balance > 0, transferToIban must be provided: funds are moved atomically before deletion.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Account closed"),
        @ApiResponse(responseCode = "400", description = "Account has funds but no destination provided"),
        @ApiResponse(responseCode = "403", description = "Account does not belong to authenticated user"),
        @ApiResponse(responseCode = "404", description = "Account or destination not found")
    })
    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestBody @Valid DeleteAccountRequest request) {
        accountService.delete(request.iban(), request.transferToIban(), getAuthenticatedUser());
        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}

package com.mehdi.banking_api.controller;

import com.mehdi.banking_api.dto.request.CreateBeneficiaryRequest;
import com.mehdi.banking_api.dto.request.ValidateBeneficiaryRequest;
import com.mehdi.banking_api.dto.response.BeneficiaryResponse;
import com.mehdi.banking_api.dto.response.BeneficiaryValidationResponse;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.service.BeneficiaryService;
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

@Tag(name = "Beneficiaries", description = "Manage transfer beneficiaries for the authenticated user")
@SecurityRequirement(name = "jwt-cookie")
@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @Operation(summary = "List beneficiaries", description = "Returns all beneficiaries saved by the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List returned"),
        @ApiResponse(responseCode = "403", description = "Not authenticated")
    })
    @GetMapping
    public List<BeneficiaryResponse> getAll() {
        return beneficiaryService.findAllByUser(getAuthenticatedUser());
    }

    @Operation(summary = "Validate beneficiary", description = "Validates the IBAN and checks name against the account owner when the IBAN is in this system.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Validation result returned"),
        @ApiResponse(responseCode = "400", description = "Invalid IBAN"),
        @ApiResponse(responseCode = "403", description = "Not authenticated")
    })
    @PostMapping("/validate")
    public ResponseEntity<BeneficiaryValidationResponse> validate(@RequestBody @Valid ValidateBeneficiaryRequest request) {
        return ResponseEntity.ok(beneficiaryService.validate(
                request.getIban(),
                request.getFirstName(),
                request.getLastName()
        ));
    }

    @Operation(summary = "Create beneficiary", description = "Saves a new beneficiary linked to the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Beneficiary created"),
        @ApiResponse(responseCode = "400", description = "Invalid IBAN"),
        @ApiResponse(responseCode = "403", description = "Not authenticated")
    })
    @PostMapping
    public ResponseEntity<BeneficiaryResponse> create(@RequestBody @Valid CreateBeneficiaryRequest request) {
        return ResponseEntity.status(201).body(beneficiaryService.create(getAuthenticatedUser(), request));
    }

    @Operation(summary = "Delete beneficiary", description = "Deletes a beneficiary owned by the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "403", description = "Not your beneficiary"),
        @ApiResponse(responseCode = "404", description = "Beneficiary not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable java.util.UUID id) {
        beneficiaryService.delete(id, getAuthenticatedUser());
        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

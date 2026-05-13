package com.mehdi.banking_api.controller;

import com.mehdi.banking_api.dto.response.UserResponse;
import com.mehdi.banking_api.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "Retrieve authenticated user information")
@SecurityRequirement(name = "jwt-cookie")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Operation(summary = "Get current user", description = "Returns the profile of the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile returned"),
        @ApiResponse(responseCode = "403", description = "Not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName()));
    }
}

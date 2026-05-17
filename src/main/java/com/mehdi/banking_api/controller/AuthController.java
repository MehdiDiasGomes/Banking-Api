package com.mehdi.banking_api.controller;

import com.mehdi.banking_api.dto.request.LoginRequest;
import com.mehdi.banking_api.dto.request.RegisterRequest;
import com.mehdi.banking_api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Register and login — JWT delivered as HttpOnly cookie")
@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Login", description = "Authenticate with email and password. Sets a JWT cookie on success.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated"),
        @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        String token = authService.login(request);
        addJwtCookie(response, token);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Register", description = "Create a new account. Sets a JWT cookie on success.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "500", description = "Email already in use")
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request, HttpServletResponse response) {
        String token = authService.register(request);
        addJwtCookie(response, token);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Logout", description = "Clears the JWT cookie.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logged out")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        setJwtCookie(response, "", 0);
        return ResponseEntity.ok().build();
    }

    private void addJwtCookie(HttpServletResponse response, String token) {
        setJwtCookie(response, token, 86400);
    }

    private void setJwtCookie(HttpServletResponse response, String value, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from("jwt", value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAge)
                .sameSite("None")
                .domain(".mdiasgomes.com")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}

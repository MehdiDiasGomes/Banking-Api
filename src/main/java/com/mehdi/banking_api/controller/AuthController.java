package com.mehdi.banking_api.controller;

import com.mehdi.banking_api.dto.request.LoginRequest;
import com.mehdi.banking_api.dto.request.RegisterRequest;
import com.mehdi.banking_api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Authentication", description = "Register and login — JWT delivered as HttpOnly cookie")
@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.domain}")
    private String cookieDomain;

    @Value("${app.cookie.same-site}")
    private String cookieSameSite;

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

    @Operation(summary = "Register", description = "Create a new account. A verification email is sent to the provided address.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created — verification email sent"),
        @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Verify email", description = "Activates the account linked to the given verification token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account verified"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @GetMapping("/verify")
    public ResponseEntity<Void> verify(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
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
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("jwt", value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAge)
                .sameSite(cookieSameSite);

        if (!cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }
}

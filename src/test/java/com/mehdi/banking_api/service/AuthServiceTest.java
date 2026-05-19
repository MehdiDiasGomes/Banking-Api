package com.mehdi.banking_api.service;

import com.mehdi.banking_api.dto.request.LoginRequest;
import com.mehdi.banking_api.dto.request.RegisterRequest;
import com.mehdi.banking_api.exception.BusinessException;
import com.mehdi.banking_api.exception.ResourceNotFoundException;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.repository.UserRepository;
import com.mehdi.banking_api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private MailService mailService;

    @Mock
    private BCryptPasswordEncoder encoder;

    @InjectMocks
    private AuthService authService;

    // ── login ──────────────────────────────────────────────────────────────────

    @Test
    void login_withValidCredentials_returnsToken() {
        User user = User.builder()
                .email("user@test.com")
                .password("hashed-password")
                .verified(true)
                .build();

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(encoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("user@test.com")).thenReturn("mock-token");

        String token = authService.login(request);

        assertThat(token).isEqualTo("mock-token");
    }

    @Test
    void login_withUnknownEmail_throwsResourceNotFoundException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void login_withWrongPassword_throwsBusinessException() {
        User user = User.builder()
                .email("user@test.com")
                .password("hashed-password")
                .verified(true)
                .build();

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Incorrect password");
    }

    @Test
    void login_withUnverifiedAccount_throwsBusinessException() {
        User user = User.builder()
                .email("user@test.com")
                .password("hashed-password")
                .verified(false)
                .build();

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(encoder.matches("password123", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Please verify your email before logging in");
    }

    // ── register ───────────────────────────────────────────────────────────────

    @Test
    void register_savesUserAndSendsVerificationEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@test.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        authService.register(request);

        verify(userRepository).save(any(User.class));
        verify(mailService).sendVerificationEmail(eq("new@test.com"), eq("John"), any(String.class));
    }

    // ── verifyEmail ────────────────────────────────────────────────────────────

    @Test
    void verifyEmail_withValidToken_activatesAccount() {
        String token = UUID.randomUUID().toString();
        User user = User.builder()
                .email("user@test.com")
                .verificationToken(token)
                .tokenExpiresAt(LocalDateTime.now().plusHours(1))
                .verified(false)
                .build();

        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(user));

        authService.verifyEmail(token);

        assertThat(user.isVerified()).isTrue();
        assertThat(user.getVerificationToken()).isNull();
        assertThat(user.getTokenExpiresAt()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmail_withExpiredToken_throwsBusinessException() {
        String token = UUID.randomUUID().toString();
        User user = User.builder()
                .email("user@test.com")
                .verificationToken(token)
                .tokenExpiresAt(LocalDateTime.now().minusHours(1))
                .verified(false)
                .build();

        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmail(token))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Verification token has expired");
    }

    @Test
    void verifyEmail_withUnknownToken_throwsBusinessException() {
        when(userRepository.findByVerificationToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("invalid-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid verification token");
    }
}

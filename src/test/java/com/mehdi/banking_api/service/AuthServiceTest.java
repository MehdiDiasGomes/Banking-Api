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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void login_withValidCredentials_returnsToken() {
        String rawPassword = "password123";
        User user = User.builder()
                .email("user@test.com")
                .password(encoder.encode(rawPassword))
                .build();

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword(rawPassword);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
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
                .password(encoder.encode("correct-password"))
                .build();

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mot de passe incorrect");
    }

    @Test
    void register_savesUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@test.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        when(jwtService.generateToken("new@test.com")).thenReturn("new-token");

        String token = authService.register(request);

        assertThat(token).isEqualTo("new-token");
        verify(userRepository).save(any(User.class));
    }
}

package com.mehdi.banking_api.service;

import com.mehdi.banking_api.dto.request.LoginRequest;
import com.mehdi.banking_api.dto.request.RegisterRequest;
import com.mehdi.banking_api.exception.BusinessException;
import com.mehdi.banking_api.exception.ResourceNotFoundException;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.repository.UserRepository;
import com.mehdi.banking_api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final MailService mailService;
    private final BCryptPasswordEncoder encoder;

    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!encoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Incorrect password");
        }

        if (!user.isVerified()) {
            throw new BusinessException("Please verify your email before logging in");
        }

        return jwtService.generateToken(user.getEmail());
    }

    @Transactional
    public void register(RegisterRequest request) {
        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .email(request.getEmail())
                .password(encoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .verificationToken(verificationToken)
                .tokenExpiresAt(LocalDateTime.now().plusHours(24))
                .build();

        userRepository.save(user);
        mailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), verificationToken);
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BusinessException("Invalid verification token"));

        if (LocalDateTime.now().isAfter(user.getTokenExpiresAt())) {
            throw new BusinessException("Verification token has expired");
        }

        user.setVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiresAt(null);
        userRepository.save(user);
    }
}

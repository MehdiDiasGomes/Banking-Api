package com.mehdi.banking_api.service;

import com.mehdi.banking_api.dto.request.LoginRequest;
import com.mehdi.banking_api.dto.request.RegisterRequest;
import com.mehdi.banking_api.dto.response.AuthResponse;
import com.mehdi.banking_api.exception.BusinessException;
import com.mehdi.banking_api.exception.ResourceNotFoundException;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.repository.UserRepository;
import com.mehdi.banking_api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!encoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Mot de passe incorrect");
        }

        return jwtService.generateToken(user.getEmail());
    }

    public String register(RegisterRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .password(encoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        userRepository.save(user);
        return jwtService.generateToken(user.getEmail());
    }
}

package com.mehdi.banking_api.service;

import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public User save (User user) {
        return userRepository.save(user);
    }
}

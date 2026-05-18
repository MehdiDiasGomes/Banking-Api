package com.mehdi.banking_api.service;

import com.mehdi.banking_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final UserRepository userRepository;

    /**
     * Deletes unverified accounts whose verification token has expired.
     * Runs every day at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteExpiredUnverifiedAccounts() {
        userRepository.deleteExpiredUnverifiedUsers(LocalDateTime.now());
        log.info("Expired unverified accounts cleanup completed at {}", LocalDateTime.now());
    }
}

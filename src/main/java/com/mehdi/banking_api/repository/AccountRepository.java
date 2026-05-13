package com.mehdi.banking_api.repository;

import com.mehdi.banking_api.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByOwnerId(UUID ownerId);
    Optional<Account> findByIban(String iban);
}

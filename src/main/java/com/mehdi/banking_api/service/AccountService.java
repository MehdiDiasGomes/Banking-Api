package com.mehdi.banking_api.service;

import com.mehdi.banking_api.dto.request.CreateAccountRequest;
import com.mehdi.banking_api.dto.response.AccountResponse;
import com.mehdi.banking_api.exception.ForbiddenException;
import com.mehdi.banking_api.model.Account;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.mehdi.banking_api.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    private String generateIban() {
        return "LU" + System.currentTimeMillis();
    }

    public AccountResponse save(User user, CreateAccountRequest request) {
        Account account = Account.builder()
                .iban(generateIban())
                .balance(BigDecimal.ZERO)
                .type(request.getAccountType())
                .owner(user)
                .build();

        Account saved = accountRepository.save(account);

        return new AccountResponse(saved.getId(), saved.getIban(), saved.getBalance(), saved.getType());
    }

    public List<AccountResponse> findAllByUser(User owner) {
        return accountRepository.findByOwnerId(owner.getId())
                .stream()
                .map(a -> new AccountResponse(a.getId(), a.getIban(), a.getBalance(), a.getType()))
                .toList();
    }

    public Account findEntityById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    public AccountResponse deposit(String iban, BigDecimal amount, User connectedUser) {
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + iban));

        if (!account.getOwner().getId().equals(connectedUser.getId())) {
            throw new ForbiddenException();
        }

        account.setBalance(account.getBalance().add(amount));
        Account saved = accountRepository.save(account);

        return new AccountResponse(saved.getId(), saved.getIban(), saved.getBalance(), saved.getType());
    }
}

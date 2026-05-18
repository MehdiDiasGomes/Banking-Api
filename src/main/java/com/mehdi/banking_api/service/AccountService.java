package com.mehdi.banking_api.service;

import com.mehdi.banking_api.dto.request.CreateAccountRequest;
import com.mehdi.banking_api.dto.response.AccountResponse;
import com.mehdi.banking_api.exception.BusinessException;
import com.mehdi.banking_api.exception.ForbiddenException;
import com.mehdi.banking_api.model.Account;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.repository.AccountRepository;
import com.mehdi.banking_api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mehdi.banking_api.exception.ResourceNotFoundException;

import org.iban4j.CountryCode;
import org.iban4j.Iban;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    private String generateIban() {
        // Account number: last 13 digits of current millis, zero-padded (LU BBAN = 3 bank + 13 account)
        String accountNumber = String.format("%013d", System.currentTimeMillis() % 10_000_000_000_000L);
        return new Iban.Builder()
                .countryCode(CountryCode.LU)
                .bankCode("001")
                .accountNumber(accountNumber)
                .build()
                .toString();
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

    @Transactional
    public void delete(String iban, String transferToIban, User connectedUser) {
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + iban));

        if (!account.getOwner().getId().equals(connectedUser.getId())) {
            throw new ForbiddenException();
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            if (transferToIban == null || transferToIban.isBlank()) {
                throw new BusinessException("Account has funds. Provide a destination account to transfer the balance before closing.");
            }

            Account destination = accountRepository.findByIban(transferToIban)
                    .orElseThrow(() -> new ResourceNotFoundException("Destination account not found: " + transferToIban));

            if (!destination.getOwner().getId().equals(connectedUser.getId())) {
                throw new ForbiddenException("Destination account does not belong to you.");
            }

            destination.setBalance(destination.getBalance().add(account.getBalance()));
            account.setBalance(BigDecimal.ZERO);
            accountRepository.save(destination);
            accountRepository.save(account);
        }

        transactionRepository.deleteAll(transactionRepository.findBySenderOrReceiver(account, account));
        accountRepository.delete(account);
    }
}

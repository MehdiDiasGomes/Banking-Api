package com.mehdi.banking_api.service;

import com.mehdi.banking_api.dto.request.TransferRequest;
import com.mehdi.banking_api.dto.response.TransactionResponse;
import com.mehdi.banking_api.exception.BusinessException;
import com.mehdi.banking_api.exception.ForbiddenException;
import com.mehdi.banking_api.exception.ResourceNotFoundException;
import com.mehdi.banking_api.model.*;
import com.mehdi.banking_api.repository.AccountRepository;
import com.mehdi.banking_api.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionService transactionService;

    private User buildUser(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("encoded")
                .build();
    }

    private Account buildAccount(String iban, double balance, User owner) {
        return Account.builder()
                .id(UUID.randomUUID())
                .iban(iban)
                .balance(balance)
                .type(AccountType.SAVINGS)
                .owner(owner)
                .build();
    }

    @Test
    void getHistory_withOwnAccount_returnsTransactions() {
        User user = buildUser("user@test.com");
        Account account = buildAccount("LU111", 500.0, user);

        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .amount(100.0)
                .sender(account)
                .receiver(account)
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        when(accountRepository.findByIban("LU111")).thenReturn(Optional.of(account));
        when(transactionRepository.findBySenderOrReceiver(account, account)).thenReturn(List.of(tx));

        List<TransactionResponse> result = transactionService.getHistory("LU111", user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSenderIban()).isEqualTo("LU111");
    }

    @Test
    void getHistory_withUnknownIban_throwsResourceNotFoundException() {
        User user = buildUser("user@test.com");
        when(accountRepository.findByIban("LU000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getHistory("LU000", user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("LU000");
    }

    @Test
    void getHistory_withForeignAccount_throwsBusinessException() {
        User owner = buildUser("owner@test.com");
        User attacker = buildUser("attacker@test.com");
        Account account = buildAccount("LU111", 500.0, owner);

        when(accountRepository.findByIban("LU111")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.getHistory("LU111", attacker))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void transfer_withValidRequest_completesAndReturnsResponse() {
        User user = buildUser("user@test.com");
        Account sender = buildAccount("LU111", 500.0, user);
        Account receiver = buildAccount("LU222", 100.0, buildUser("other@test.com"));

        TransferRequest request = new TransferRequest();
        request.setSenderIban("LU111");
        request.setReceiverIban("LU222");
        request.setAmount(200.0);

        Transaction saved = Transaction.builder()
                .id(UUID.randomUUID())
                .amount(200.0)
                .sender(sender)
                .receiver(receiver)
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        when(accountRepository.findByIban("LU111")).thenReturn(Optional.of(sender));
        when(accountRepository.findByIban("LU222")).thenReturn(Optional.of(receiver));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        TransactionResponse response = transactionService.transfer(user, request);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualTo(200.0);
        assertThat(sender.getBalance()).isEqualTo(300.0);
        assertThat(receiver.getBalance()).isEqualTo(300.0);
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void transfer_withUnauthorizedSender_throwsBusinessException() {
        User user = buildUser("user@test.com");
        User realOwner = buildUser("owner@test.com");
        Account sender = buildAccount("LU111", 500.0, realOwner);
        Account receiver = buildAccount("LU222", 100.0, user);

        TransferRequest request = new TransferRequest();
        request.setSenderIban("LU111");
        request.setReceiverIban("LU222");
        request.setAmount(100.0);

        when(accountRepository.findByIban("LU111")).thenReturn(Optional.of(sender));
        when(accountRepository.findByIban("LU222")).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> transactionService.transfer(user, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("transfer");
    }

    @Test
    void transfer_withInsufficientBalance_throwsBusinessException() {
        User user = buildUser("user@test.com");
        Account sender = buildAccount("LU111", 50.0, user);
        Account receiver = buildAccount("LU222", 0.0, buildUser("other@test.com"));

        TransferRequest request = new TransferRequest();
        request.setSenderIban("LU111");
        request.setReceiverIban("LU222");
        request.setAmount(200.0);

        when(accountRepository.findByIban("LU111")).thenReturn(Optional.of(sender));
        when(accountRepository.findByIban("LU222")).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> transactionService.transfer(user, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Insufficient balance");
    }

    @Test
    void transfer_withUnknownSenderIban_throwsResourceNotFoundException() {
        User user = buildUser("user@test.com");
        TransferRequest request = new TransferRequest();
        request.setSenderIban("LU_UNKNOWN");
        request.setReceiverIban("LU222");
        request.setAmount(100.0);

        when(accountRepository.findByIban("LU_UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.transfer(user, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

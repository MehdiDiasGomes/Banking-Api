package com.mehdi.banking_api.service;

import com.mehdi.banking_api.dto.request.CreateAccountRequest;
import com.mehdi.banking_api.dto.response.AccountResponse;
import com.mehdi.banking_api.exception.BusinessException;
import com.mehdi.banking_api.exception.ForbiddenException;
import com.mehdi.banking_api.exception.ResourceNotFoundException;
import com.mehdi.banking_api.model.Account;
import com.mehdi.banking_api.model.AccountType;
import com.mehdi.banking_api.model.Transaction;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.repository.AccountRepository;
import com.mehdi.banking_api.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .password("encoded")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Test
    void save_createsAccountAndReturnsResponse() {
        User user = buildUser();
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(AccountType.SAVINGS);

        Account saved = Account.builder()
                .id(UUID.randomUUID())
                .iban("LU123456789")
                .balance(BigDecimal.ZERO)
                .type(AccountType.SAVINGS)
                .owner(user)
                .build();

        when(accountRepository.save(any(Account.class))).thenReturn(saved);

        AccountResponse response = accountService.save(user, request);

        assertThat(response.getIban()).isEqualTo("LU123456789");
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getType()).isEqualTo(AccountType.SAVINGS);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isEqualTo(user);
        assertThat(captor.getValue().getIban()).startsWith("XX");
    }

    @Test
    void findAllByUser_returnsAccountsForOwner() {
        User user = buildUser();
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .iban("LU111")
                .balance(new BigDecimal("100.0"))
                .type(AccountType.CHECKING)
                .owner(user)
                .build();

        when(accountRepository.findByOwnerId(user.getId())).thenReturn(List.of(account));

        List<AccountResponse> result = accountService.findAllByUser(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIban()).isEqualTo("LU111");
    }

    @Test
    void findEntityById_withExistingId_returnsAccount() {
        UUID id = UUID.randomUUID();
        Account account = Account.builder().id(id).iban("LU999").build();

        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        Account result = accountService.findEntityById(id);

        assertThat(result.getIban()).isEqualTo("LU999");
    }

    @Test
    void findEntityById_withUnknownId_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findEntityById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── delete ───────────────────────────────────────────────────────────────────

    @Test
    void delete_zeroBalance_deletesAccountAndItsTransactions() {
        User user = buildUser();
        Account account = Account.builder()
                .id(UUID.randomUUID()).iban("LU001").balance(BigDecimal.ZERO)
                .type(AccountType.CHECKING).owner(user).build();

        when(accountRepository.findByIban("LU001")).thenReturn(Optional.of(account));
        when(transactionRepository.findBySenderOrReceiver(account, account)).thenReturn(List.of());

        accountService.delete("LU001", null, user);

        verify(transactionRepository).deleteAll(List.of());
        verify(accountRepository).delete(account);
    }

    @Test
    void delete_withBalance_transfersToDestinationThenDeletes() {
        User user = buildUser();
        Account source = Account.builder()
                .id(UUID.randomUUID()).iban("LU001").balance(new BigDecimal("500.00"))
                .type(AccountType.CHECKING).owner(user).build();
        Account destination = Account.builder()
                .id(UUID.randomUUID()).iban("LU002").balance(new BigDecimal("100.00"))
                .type(AccountType.SAVINGS).owner(user).build();

        when(accountRepository.findByIban("LU001")).thenReturn(Optional.of(source));
        when(accountRepository.findByIban("LU002")).thenReturn(Optional.of(destination));
        when(transactionRepository.findBySenderOrReceiver(source, source)).thenReturn(List.of());

        accountService.delete("LU001", "LU002", user);

        assertThat(destination.getBalance()).isEqualByComparingTo("600.00");
        assertThat(source.getBalance()).isEqualByComparingTo("0");

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Account::getIban)
                .containsExactlyInAnyOrder("LU002", "LU001");

        verify(accountRepository).delete(source);
    }

    @Test
    void delete_withBalance_noTransferIban_throwsBusinessException() {
        User user = buildUser();
        Account account = Account.builder()
                .id(UUID.randomUUID()).iban("LU001").balance(new BigDecimal("200.00"))
                .type(AccountType.CHECKING).owner(user).build();

        when(accountRepository.findByIban("LU001")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.delete("LU001", null, user))
                .isInstanceOf(BusinessException.class);

        verify(accountRepository, never()).delete(any());
    }

    @Test
    void delete_withBalance_blankTransferIban_throwsBusinessException() {
        User user = buildUser();
        Account account = Account.builder()
                .id(UUID.randomUUID()).iban("LU001").balance(new BigDecimal("50.00"))
                .type(AccountType.CHECKING).owner(user).build();

        when(accountRepository.findByIban("LU001")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.delete("LU001", "   ", user))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_accountNotOwnedByUser_throwsForbiddenException() {
        User owner = buildUser();
        User attacker = User.builder().id(UUID.randomUUID()).build();
        Account account = Account.builder()
                .id(UUID.randomUUID()).iban("LU001").balance(BigDecimal.ZERO)
                .owner(owner).build();

        when(accountRepository.findByIban("LU001")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.delete("LU001", null, attacker))
                .isInstanceOf(ForbiddenException.class);

        verify(accountRepository, never()).delete(any());
    }

    @Test
    void delete_destinationNotFound_throwsResourceNotFoundException() {
        User user = buildUser();
        Account source = Account.builder()
                .id(UUID.randomUUID()).iban("LU001").balance(new BigDecimal("100.00"))
                .owner(user).build();

        when(accountRepository.findByIban("LU001")).thenReturn(Optional.of(source));
        when(accountRepository.findByIban("LU999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.delete("LU001", "LU999", user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("LU999");
    }

    @Test
    void delete_destinationOwnedByAnotherUser_throwsForbiddenException() {
        User user = buildUser();
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        Account source = Account.builder()
                .id(UUID.randomUUID()).iban("LU001").balance(new BigDecimal("100.00"))
                .owner(user).build();
        Account destination = Account.builder()
                .id(UUID.randomUUID()).iban("LU002").balance(BigDecimal.ZERO)
                .owner(otherUser).build();

        when(accountRepository.findByIban("LU001")).thenReturn(Optional.of(source));
        when(accountRepository.findByIban("LU002")).thenReturn(Optional.of(destination));

        assertThatThrownBy(() -> accountService.delete("LU001", "LU002", user))
                .isInstanceOf(ForbiddenException.class);

        verify(accountRepository, never()).delete(any());
    }

    @Test
    void delete_accountNotFound_throwsResourceNotFoundException() {
        User user = buildUser();
        when(accountRepository.findByIban("LU404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.delete("LU404", null, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("LU404");
    }

    @Test
    void delete_withBalance_deletesTransactionsBeforeAccount() {
        User user = buildUser();
        Account source = Account.builder()
                .id(UUID.randomUUID()).iban("LU001").balance(new BigDecimal("100.00"))
                .owner(user).build();
        Account destination = Account.builder()
                .id(UUID.randomUUID()).iban("LU002").balance(BigDecimal.ZERO)
                .owner(user).build();
        Transaction tx = Transaction.builder().id(UUID.randomUUID()).build();

        when(accountRepository.findByIban("LU001")).thenReturn(Optional.of(source));
        when(accountRepository.findByIban("LU002")).thenReturn(Optional.of(destination));
        when(transactionRepository.findBySenderOrReceiver(source, source)).thenReturn(List.of(tx));

        accountService.delete("LU001", "LU002", user);

        verify(transactionRepository).deleteAll(List.of(tx));
        verify(accountRepository).delete(source);
    }
}

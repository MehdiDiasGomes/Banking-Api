package com.mehdi.banking_api.service;

import com.mehdi.banking_api.dto.request.CreateAccountRequest;
import com.mehdi.banking_api.dto.response.AccountResponse;
import com.mehdi.banking_api.exception.ResourceNotFoundException;
import com.mehdi.banking_api.model.Account;
import com.mehdi.banking_api.model.AccountType;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
                .balance(0.0)
                .type(AccountType.SAVINGS)
                .owner(user)
                .build();

        when(accountRepository.save(any(Account.class))).thenReturn(saved);

        AccountResponse response = accountService.save(user, request);

        assertThat(response.getIban()).isEqualTo("LU123456789");
        assertThat(response.getBalance()).isEqualTo(0.0);
        assertThat(response.getType()).isEqualTo(AccountType.SAVINGS);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isEqualTo(user);
        assertThat(captor.getValue().getIban()).startsWith("LU");
    }

    @Test
    void findAllByUser_returnsAccountsForOwner() {
        User user = buildUser();
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .iban("LU111")
                .balance(100.0)
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
}

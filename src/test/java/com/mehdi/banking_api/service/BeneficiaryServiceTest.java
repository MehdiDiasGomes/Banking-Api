package com.mehdi.banking_api.service;

import com.mehdi.banking_api.dto.request.CreateBeneficiaryRequest;
import com.mehdi.banking_api.dto.response.BeneficiaryResponse;
import com.mehdi.banking_api.dto.response.BeneficiaryValidationResponse;
import com.mehdi.banking_api.exception.ForbiddenException;
import com.mehdi.banking_api.exception.ResourceNotFoundException;
import com.mehdi.banking_api.model.Account;
import com.mehdi.banking_api.model.Beneficiary;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.repository.AccountRepository;
import com.mehdi.banking_api.repository.BeneficiaryRepository;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IbanValidationService ibanValidationService;

    @InjectMocks
    private BeneficiaryService beneficiaryService;

    private static final String VALID_IBAN = "LU280019400644750000";

    private User buildUser(String firstName, String lastName) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(firstName.toLowerCase() + "@test.com")
                .password("encoded")
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }

    private Iban parsedIban() {
        return Iban.valueOf(VALID_IBAN);
    }

    // ── validate ─────────────────────────────────────────────────────────────

    @Test
    void validate_withInternalIbanAndMatchingName_returnsNameMatchTrue() {
        User owner = buildUser("John", "Doe");
        Account account = Account.builder().iban(VALID_IBAN).owner(owner).build();
        Iban iban = parsedIban();

        when(ibanValidationService.parseAndValidate(VALID_IBAN)).thenReturn(iban);
        when(accountRepository.findByIban(iban.toString())).thenReturn(Optional.of(account));
        when(ibanValidationService.namesMatch("John", "Doe", "John", "Doe")).thenReturn(true);

        BeneficiaryValidationResponse result = beneficiaryService.validate(VALID_IBAN, "John", "Doe");

        assertThat(result.isNameMatch()).isTrue();
        assertThat(result.getBankCode()).isEqualTo(iban.getBankCode());
        assertThat(result.getCountryCode()).isEqualTo("LU");
    }

    @Test
    void validate_withInternalIbanAndMismatchedName_returnsNameMatchFalse() {
        User owner = buildUser("Jane", "Smith");
        Account account = Account.builder().iban(VALID_IBAN).owner(owner).build();
        Iban iban = parsedIban();

        when(ibanValidationService.parseAndValidate(VALID_IBAN)).thenReturn(iban);
        when(accountRepository.findByIban(iban.toString())).thenReturn(Optional.of(account));
        when(ibanValidationService.namesMatch("John", "Doe", "Jane", "Smith")).thenReturn(false);

        BeneficiaryValidationResponse result = beneficiaryService.validate(VALID_IBAN, "John", "Doe");

        assertThat(result.isNameMatch()).isFalse();
    }

    @Test
    void validate_withExternalIban_returnsNameMatchTrue() {
        // External IBAN — not found in our system → nameMatch defaults to true (no warning)
        Iban iban = parsedIban();

        when(ibanValidationService.parseAndValidate(VALID_IBAN)).thenReturn(iban);
        when(accountRepository.findByIban(iban.toString())).thenReturn(Optional.empty());

        BeneficiaryValidationResponse result = beneficiaryService.validate(VALID_IBAN, "Anyone", "Whatever");

        assertThat(result.isNameMatch()).isTrue();
        verify(ibanValidationService, never()).namesMatch(any(), any(), any(), any());
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_persistsBeneficiaryAndReturnsResponse() {
        User owner = buildUser("John", "Doe");
        Iban iban = parsedIban();

        CreateBeneficiaryRequest request = new CreateBeneficiaryRequest();
        request.setIban(VALID_IBAN);
        request.setFirstName("John");
        request.setLastName("Doe");

        Beneficiary saved = Beneficiary.builder()
                .id(UUID.randomUUID())
                .iban(iban.toString())
                .bankCode(iban.getBankCode())
                .countryCode(iban.getCountryCode().getAlpha2())
                .firstName("John")
                .lastName("Doe")
                .owner(owner)
                .createdAt(LocalDateTime.now())
                .build();

        when(ibanValidationService.parseAndValidate(VALID_IBAN)).thenReturn(iban);
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenReturn(saved);

        BeneficiaryResponse response = beneficiaryService.create(owner, request);

        assertThat(response.getIban()).isEqualTo(iban.toString());
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getCountryCode()).isEqualTo("LU");

        ArgumentCaptor<Beneficiary> captor = ArgumentCaptor.forClass(Beneficiary.class);
        verify(beneficiaryRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isEqualTo(owner);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_withOwner_deletesBeneficiary() {
        User owner = buildUser("John", "Doe");
        UUID id = UUID.randomUUID();
        Beneficiary beneficiary = Beneficiary.builder().id(id).owner(owner).build();

        when(beneficiaryRepository.findById(id)).thenReturn(Optional.of(beneficiary));

        beneficiaryService.delete(id, owner);

        verify(beneficiaryRepository).delete(beneficiary);
    }

    @Test
    void delete_withNonOwner_throwsForbiddenException() {
        User owner = buildUser("John", "Doe");
        User other = buildUser("Jane", "Smith");
        UUID id = UUID.randomUUID();
        Beneficiary beneficiary = Beneficiary.builder().id(id).owner(owner).build();

        when(beneficiaryRepository.findById(id)).thenReturn(Optional.of(beneficiary));

        assertThatThrownBy(() -> beneficiaryService.delete(id, other))
                .isInstanceOf(ForbiddenException.class);

        verify(beneficiaryRepository, never()).delete(any());
    }

    @Test
    void delete_withUnknownId_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(beneficiaryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> beneficiaryService.delete(id, buildUser("John", "Doe")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── findAllByUser ─────────────────────────────────────────────────────────

    @Test
    void findAllByUser_returnsMappedResponses() {
        User owner = buildUser("John", "Doe");
        Beneficiary b = Beneficiary.builder()
                .id(UUID.randomUUID())
                .iban(VALID_IBAN)
                .bankCode("001")
                .countryCode("LU")
                .firstName("Alice")
                .lastName("Martin")
                .owner(owner)
                .createdAt(LocalDateTime.now())
                .build();

        when(beneficiaryRepository.findByOwner(owner)).thenReturn(List.of(b));

        List<BeneficiaryResponse> result = beneficiaryService.findAllByUser(owner);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIban()).isEqualTo(VALID_IBAN);
        assertThat(result.get(0).getFirstName()).isEqualTo("Alice");
    }

    @Test
    void findAllByUser_withNoBeneficiaries_returnsEmptyList() {
        User owner = buildUser("John", "Doe");
        when(beneficiaryRepository.findByOwner(owner)).thenReturn(List.of());

        assertThat(beneficiaryService.findAllByUser(owner)).isEmpty();
    }
}

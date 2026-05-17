package com.mehdi.banking_api.service;

import com.mehdi.banking_api.dto.request.CreateBeneficiaryRequest;
import com.mehdi.banking_api.dto.response.BeneficiaryResponse;
import com.mehdi.banking_api.dto.response.BeneficiaryValidationResponse;
import com.mehdi.banking_api.exception.ForbiddenException;
import com.mehdi.banking_api.exception.ResourceNotFoundException;
import com.mehdi.banking_api.model.Beneficiary;
import com.mehdi.banking_api.model.User;
import com.mehdi.banking_api.repository.AccountRepository;
import com.mehdi.banking_api.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import org.iban4j.Iban;
import org.springframework.stereotype.Service;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final IbanValidationService ibanValidationService;

    /**
     * Validates an IBAN and checks whether the provided name matches the account owner
     * when the IBAN belongs to an account in this system. External IBANs are assumed valid.
     */
    public BeneficiaryValidationResponse validate(String iban, String firstName, String lastName) {
        Iban parsed = ibanValidationService.parseAndValidate(iban);

        boolean nameMatch = accountRepository.findByIban(parsed.toString())
                .map(account -> ibanValidationService.namesMatch(
                        firstName, lastName,
                        account.getOwner().getFirstName(),
                        account.getOwner().getLastName()))
                .orElse(true); // External IBAN — cannot verify, no warning shown

        return new BeneficiaryValidationResponse(
                parsed.getBankCode(),
                parsed.getCountryCode().getAlpha2(),
                nameMatch
        );
    }

    /** Creates and persists a beneficiary linked to the authenticated user. */
    public BeneficiaryResponse create(User owner, CreateBeneficiaryRequest request) {
        Iban parsed = ibanValidationService.parseAndValidate(request.getIban());

        Beneficiary beneficiary = Beneficiary.builder()
                .iban(parsed.toString())
                .bankCode(parsed.getBankCode())
                .countryCode(parsed.getCountryCode().getAlpha2())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .owner(owner)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(beneficiaryRepository.save(beneficiary));
    }

    /** Deletes a beneficiary by id after verifying ownership. */
    public void delete(UUID id, User owner) {
        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found: " + id));

        if (!beneficiary.getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenException("You are not allowed to delete this beneficiary");
        }

        beneficiaryRepository.delete(beneficiary);
    }

    /** Returns all beneficiaries belonging to the given user. */
    public List<BeneficiaryResponse> findAllByUser(User owner) {
        return beneficiaryRepository.findByOwner(owner).stream()
                .map(this::toResponse)
                .toList();
    }

    private BeneficiaryResponse toResponse(Beneficiary b) {
        return new BeneficiaryResponse(
                b.getId(),
                b.getIban(),
                b.getBankCode(),
                b.getCountryCode(),
                b.getFirstName(),
                b.getLastName()
        );
    }
}

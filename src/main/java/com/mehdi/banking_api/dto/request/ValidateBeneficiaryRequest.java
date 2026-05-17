package com.mehdi.banking_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateBeneficiaryRequest {

    @NotBlank
    private String iban;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;
}

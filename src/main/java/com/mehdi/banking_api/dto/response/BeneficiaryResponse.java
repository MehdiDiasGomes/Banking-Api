package com.mehdi.banking_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class BeneficiaryResponse {
    private UUID id;
    private String iban;
    private String bankCode;
    private String countryCode;
    private String firstName;
    private String lastName;
}

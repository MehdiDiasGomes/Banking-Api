package com.mehdi.banking_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BeneficiaryValidationResponse {
    private String bankCode;
    private String countryCode;
    private boolean nameMatch;
}

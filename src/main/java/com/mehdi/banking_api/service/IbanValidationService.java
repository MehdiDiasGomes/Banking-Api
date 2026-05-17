package com.mehdi.banking_api.service;

import com.mehdi.banking_api.exception.BusinessException;
import org.iban4j.Iban;
import org.iban4j.IbanFormatException;
import org.iban4j.InvalidCheckDigitException;
import org.iban4j.UnsupportedCountryException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IbanValidationService {

    /**
     * Parses and validates an IBAN string, stripping whitespace beforehand.
     *
     * @throws BusinessException if the IBAN is malformed or has an invalid checksum
     */
    public Iban parseAndValidate(String raw) {
        try {
            return Iban.valueOf(raw.replaceAll("\\s", ""));
        } catch (IbanFormatException | InvalidCheckDigitException | UnsupportedCountryException e) {
            throw new BusinessException("Invalid IBAN: " + e.getMessage());
        }
    }

    /**
     * Compares two full names using token-set matching after Unicode normalization.
     * Tolerates order inversion (e.g. "John DOE" == "DOE John") and case/accent differences.
     */
    public boolean namesMatch(String inputFirst, String inputLast, String storedFirst, String storedLast) {
        Set<String> inputTokens = tokenize(inputFirst + " " + inputLast);
        Set<String> storedTokens = tokenize(storedFirst + " " + storedLast);
        return inputTokens.equals(storedTokens);
    }

    private Set<String> tokenize(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
        return Arrays.stream(normalized.split("\\s+"))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toSet());
    }
}

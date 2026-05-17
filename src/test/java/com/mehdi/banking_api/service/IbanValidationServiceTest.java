package com.mehdi.banking_api.service;

import com.mehdi.banking_api.exception.BusinessException;
import org.iban4j.Iban;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IbanValidationServiceTest {

    private final IbanValidationService service = new IbanValidationService();

    // ── parseAndValidate ──────────────────────────────────────────────────────

    @Test
    void parseAndValidate_withValidIban_returnsIban() {
        Iban result = service.parseAndValidate("LU280019400644750000");

        assertThat(result).isNotNull();
        assertThat(result.getCountryCode().getAlpha2()).isEqualTo("LU");
    }

    @Test
    void parseAndValidate_withSpacedIban_stripsSpacesAndSucceeds() {
        Iban result = service.parseAndValidate("LU28 0019 4006 4475 0000");

        assertThat(result).isNotNull();
    }

    @Test
    void parseAndValidate_withInvalidIban_throwsBusinessException() {
        assertThatThrownBy(() -> service.parseAndValidate("INVALID"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid IBAN");
    }

    @Test
    void parseAndValidate_withBadChecksum_throwsBusinessException() {
        // Valid format but wrong check digits
        assertThatThrownBy(() -> service.parseAndValidate("LU000019400644750000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid IBAN");
    }

    // ── namesMatch ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} {1} matches {2} {3}")
    @CsvSource({
        "John, Doe, John, Doe",           // exact match
        "JOHN, DOE, john, doe",           // case difference
        "Doe, John, John, Doe",           // order inversion (first ↔ last)
        "DOE, John, john, DOE",           // mixed case + order inversion
        "Élodie, Müller, elodie, muller", // accent normalization
    })
    void namesMatch_withEquivalentNames_returnsTrue(
            String inputFirst, String inputLast, String storedFirst, String storedLast) {
        assertThat(service.namesMatch(inputFirst, inputLast, storedFirst, storedLast)).isTrue();
    }

    @Test
    void namesMatch_withDifferentNames_returnsFalse() {
        assertThat(service.namesMatch("John", "Doe", "Jane", "Doe")).isFalse();
    }

    @Test
    void namesMatch_withCompletelyDifferentNames_returnsFalse() {
        assertThat(service.namesMatch("Alice", "Smith", "Bob", "Jones")).isFalse();
    }
}

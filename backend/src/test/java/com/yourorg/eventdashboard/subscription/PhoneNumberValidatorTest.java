package com.yourorg.eventdashboard.subscription;

import com.yourorg.eventdashboard.shared.InvalidPhoneNumberException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberValidatorTest {

    @Test
    void convertsUsNumberToE164() {
        String result = PhoneNumberValidator.toE164("(415) 555-2671");
        assertThat(result).isEqualTo("+14155552671");
    }

    @Test
    void acceptsAlreadyFormattedE164() {
        String result = PhoneNumberValidator.toE164("+14155552671");
        assertThat(result).isEqualTo("+14155552671");
    }

    @Test
    void acceptsTenDigitUsNumber() {
        String result = PhoneNumberValidator.toE164("4155552671");
        assertThat(result).isEqualTo("+14155552671");
    }

    @Test
    void throwsForGarbageInput() {
        assertThatThrownBy(() -> PhoneNumberValidator.toE164("not-a-number"))
                .isInstanceOf(InvalidPhoneNumberException.class);
    }

    @Test
    void throwsForInvalidNumber() {
        assertThatThrownBy(() -> PhoneNumberValidator.toE164("+1000000000"))
                .isInstanceOf(InvalidPhoneNumberException.class);
    }
}

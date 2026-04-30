package com.yourorg.eventdashboard.subscription;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.yourorg.eventdashboard.shared.InvalidPhoneNumberException;

class PhoneNumberValidator {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
    private static final String DEFAULT_REGION = "US";

    private PhoneNumberValidator() {}

    static String toE164(String rawPhoneNumber) {
        try {
            Phonenumber.PhoneNumber parsed = PHONE_UTIL.parse(rawPhoneNumber, DEFAULT_REGION);
            if (!PHONE_UTIL.isValidNumber(parsed)) {
                throw new InvalidPhoneNumberException("Invalid phone number: " + rawPhoneNumber);
            }
            return PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new InvalidPhoneNumberException("Invalid phone number: " + rawPhoneNumber);
        }
    }
}

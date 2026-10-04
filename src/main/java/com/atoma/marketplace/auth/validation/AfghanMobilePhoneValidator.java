package com.atoma.marketplace.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class AfghanMobilePhoneValidator implements ConstraintValidator<AfghanMobilePhone, String> {

    /** Afghan mobile: +937XXXXXXXX (9 digits after 937) */
    private static final Pattern AFGHAN_MOBILE = Pattern.compile("^\\+937\\d{8}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return AFGHAN_MOBILE.matcher(normalize(value)).matches();
    }

    public static String normalize(String raw) {
        var trimmed = raw.trim().replaceAll("\\s", "");
        if (trimmed.startsWith("+")) {
            return trimmed;
        }
        if (trimmed.startsWith("937")) {
            return "+" + trimmed;
        }
        if (trimmed.startsWith("0")) {
            return "+93" + trimmed.substring(1);
        }
        return "+93" + trimmed;
    }
}

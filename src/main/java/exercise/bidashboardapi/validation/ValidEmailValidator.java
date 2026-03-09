package exercise.bidashboardapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

    // RFC 5322 compliant email regex (simplified)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // Blacklisted domains (example)
    private static final String[] BLACKLISTED_DOMAINS = {
            "tempmail.com",
            "throwaway.email",
            "guerrillamail.com"
    };

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isEmpty()) {
            return true;
        }

        // Check format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }

        // Check blacklisted domains
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        for (String blacklisted : BLACKLISTED_DOMAINS) {
            if (domain.equals(blacklisted)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Email domain is not allowed"
                ).addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}

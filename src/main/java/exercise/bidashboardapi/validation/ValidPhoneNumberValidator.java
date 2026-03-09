package exercise.bidashboardapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    // Accepts formats: +1234567890, 1234567890, 123-456-7890, (123) 456-7890
    private static final String PHONE_PATTERN = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$";

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return true;
        }

        // Remove common separators for validation
        String cleaned = phoneNumber.replaceAll("[\\s()\\-.]", "");

        // Check length (minimum 7, maximum 15 digits)
        if (cleaned.length() < 7 || cleaned.length() > 15) {
            return false;
        }

        // Check pattern
        return phoneNumber.matches(PHONE_PATTERN);
    }
}

package exercise.bidashboardapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoSpecialCharactersValidator implements ConstraintValidator<NoSpecialCharacters, String> {

    // Only allow letters, numbers, spaces, hyphens, and underscores
    private static final String ALLOWED_PATTERN = "^[a-zA-Z0-9 _-]*$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;  // Use @NotBlank for null/empty checks
        }

        // Check against pattern
        return value.matches(ALLOWED_PATTERN);
    }
}

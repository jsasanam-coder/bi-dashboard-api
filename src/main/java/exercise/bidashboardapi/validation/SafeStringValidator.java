package exercise.bidashboardapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SafeStringValidator implements ConstraintValidator<SafeString, String> {

    // Patterns that indicate potential XSS attacks
    private static final String[] DANGEROUS_PATTERNS = {
            "<script",
            "javascript:",
            "onerror=",
            "onclick=",
            "onload=",
            "<iframe",
            "eval\\(",
            "expression\\("
    };

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        String lowerValue = value.toLowerCase();

        // Check for dangerous patterns
        for (String pattern : DANGEROUS_PATTERNS) {
            if (lowerValue.contains(pattern.toLowerCase())) {
                return false;
            }
        }

        return true;
    }
}

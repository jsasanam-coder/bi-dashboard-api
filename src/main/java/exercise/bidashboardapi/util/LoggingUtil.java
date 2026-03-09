package exercise.bidashboardapi.util;

import org.slf4j.MDC;

import java.util.Locale;

public class LoggingUtil {

    private static final String CORRELATION_ID_KEY = "correlationId";

    public static String getCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        return correlationId != null ? correlationId : "NO_CORRELATION_ID";
    }

    public static void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
    }

    public static void clearCorrelationId() {
        MDC.remove(CORRELATION_ID_KEY);
    }

    /**
     * Sanitizes sensitive data by masking the middle portion
     * @param value The value to sanitize
     * @return Masked value showing only first 2 and last 2 characters
     */
    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    /**
     * Checks if a field name suggests sensitive data
     * @param fieldName The field name to check
     * @return true if field name contains sensitive keywords
     */
    public static boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        String lowerFieldName = fieldName.toLowerCase(Locale.ROOT);
        return lowerFieldName.contains("password") ||
                lowerFieldName.contains("pwd") ||
                lowerFieldName.contains("secret") ||
                lowerFieldName.contains("token") ||
                lowerFieldName.contains("apikey") ||
                lowerFieldName.contains("api_key") ||
                lowerFieldName.contains("credential") ||
                lowerFieldName.contains("auth") ||
                lowerFieldName.contains("ssn") ||
                lowerFieldName.contains("creditcard") ||
                lowerFieldName.contains("card_number");
    }

    /**
     * Sanitizes a value if the field name is sensitive
     * @param fieldName The field name
     * @param value The value
     * @return Sanitized value if sensitive, original value otherwise
     */
    public static String sanitizeIfSensitive(String fieldName, String value) {
        if (isSensitiveField(fieldName)) {
            return sanitize(value);
        }
        return value;
    }
}

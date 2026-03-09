package exercise.bidashboardapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SafeStringValidator.class)
@Documented
public @interface SafeString {

    String message() default "Field contains potentially dangerous content";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

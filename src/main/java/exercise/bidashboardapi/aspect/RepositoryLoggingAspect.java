package exercise.bidashboardapi.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Aspect
@Component
@Slf4j
public class RepositoryLoggingAspect {

    @Around("execution(* exercise.bidashboardapi.repository.*.*(..))")
    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        // Get method parameters
        Object[] args = joinPoint.getArgs();
        String parameters = formatParameters(args);

        long startTime = System.currentTimeMillis();

        try {
            log.debug("Executing {}.{}({})", className, methodName, parameters);

            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;

            // Log result summary
            String resultSummary = formatResult(result);
            log.debug("Completed {}.{} in {}ms, result: {}",
                    className, methodName, duration, resultSummary);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed {}.{} after {}ms: {}",
                    className, methodName, duration, e.getMessage(), e);
            throw e;
        }
    }

    private String formatParameters(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            if (args[i] == null) {
                sb.append("null");
            } else {
                // Avoid logging large objects
                String argStr = args[i].toString();
                if (argStr.length() > 100) {
                    sb.append(args[i].getClass().getSimpleName()).append("@")
                            .append(Integer.toHexString(args[i].hashCode()));
                } else {
                    sb.append(argStr);
                }
            }
        }
        return sb.toString();
    }

    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }

        if (result instanceof Collection<?> collection) {
            return String.format("Collection[size=%d]", collection.size());
        }

        if (result instanceof Object[] array) {
            return String.format("Array[length=%d]", array.length);
        }

        if (result instanceof java.util.Optional<?> optional) {
            return optional.isPresent() ? "Optional[present]" : "Optional[empty]";
        }

        // For other types, just return the class name
        return result.getClass().getSimpleName();
    }
}

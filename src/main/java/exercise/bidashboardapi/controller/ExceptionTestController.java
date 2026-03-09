package exercise.bidashboardapi.controller;

import exercise.bidashboardapi.entity.Customer;
import exercise.bidashboardapi.exception.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test-exceptions")
@Validated
@Slf4j
public class ExceptionTestController {

    // Test 1: ResourceNotFoundException (404)
    @GetMapping("/not-found")
    public String testNotFound() {
        log.info("Testing ResourceNotFoundException");
        throw new ResourceNotFoundException("Customer", "id", 999);
    }

    // Test 2: BadRequestException (400)
    @GetMapping("/bad-request")
    public String testBadRequest() {
        log.info("Testing BadRequestException");
        throw new BadRequestException("Invalid input provided");
    }

    // Test 3: ConflictException (409)
    @GetMapping("/conflict")
    public String testConflict() {
        log.info("Testing ConflictException");
        throw new ConflictException("Customer", "email", "duplicate@example.com");
    }

    // Test 4: InternalServerException (500)
    @GetMapping("/internal-error")
    public String testInternalError() {
        log.info("Testing InternalServerException");
        throw new InternalServerException("Database connection failed");
    }

    // Test 5: MethodArgumentNotValidException (400) - @RequestBody validation
    @PostMapping("/validate-body")
    public String testBodyValidation(@Valid @RequestBody Customer customer) {
        log.info("Testing body validation with customer: {}", customer.getName());
        return "Valid customer";
    }

    // Test 6: ConstraintViolationException (400) - @PathVariable validation
    @GetMapping("/validate-path/{id}")
    public String testPathValidation(@PathVariable @Min(value = 1, message = "ID must be at least 1") Integer id) {
        log.info("Testing path validation with id={}", id);
        return "Valid ID: " + id;
    }

    // Test 7: MissingServletRequestParameterException (400)
    @GetMapping("/missing-param")
    public String testMissingParam(@RequestParam(required = true) String name) {
        log.info("Testing missing param with name={}", name);
        return "Name: " + name;
    }

    // Test 8: MethodArgumentTypeMismatchException (400)
    @GetMapping("/type-mismatch/{id}")
    public String testTypeMismatch(@PathVariable Integer id) {
        log.info("Testing type mismatch with id={}", id);
        return "ID: " + id;
    }

    // Test 9: HttpRequestMethodNotSupportedException (405)
    // This is automatically handled by Spring when wrong HTTP method is used

    // Test 10: Generic Exception (500)
    @GetMapping("/generic-exception")
    public String testGenericException() {
        log.info("Testing generic exception");
        throw new RuntimeException("Unexpected error occurred");
    }

    // Test 11: BindException - Query param validation
    @GetMapping("/validate-query")
    public String testQueryValidation(@Valid TestRequest request) {
        log.info("Testing query validation with request: {}", request);
        return "Valid query params";
    }

    @Data
    public static class TestRequest {
        @Min(value = 1, message = "Age must be at least 1")
        private Integer age;

        @jakarta.validation.constraints.NotBlank(message = "Name is required")
        private String name;
    }
}

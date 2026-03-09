package exercise.bidashboardapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @NotNull(message = "Customer ID is required")
    @Min(value = 1, message = "Customer ID must be positive")
    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @NotNull(message = "Order date is required")
    @PastOrPresent(message = "Order date cannot be in the future")
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Total amount must not exceed 999,999.99")
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(PENDING|CONFIRMED|SHIPPED|DELIVERED|CANCELLED)$",
            message = "Status must be one of: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED")
    @Column(name = "status", length = 20)
    private String status;
}

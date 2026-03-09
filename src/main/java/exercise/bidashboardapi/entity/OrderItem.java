package exercise.bidashboardapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Integer orderItemId;

    @NotNull(message = "Order ID is required")
    @Min(value = 1, message = "Order ID must be positive")
    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @NotNull(message = "Product ID is required")
    @Min(value = 1, message = "Product ID must be positive")
    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 1000, message = "Quantity cannot exceed 1000 per item")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Price must not exceed 999,999.99")
    @Column(name = "price", nullable = false)
    private Double price;
}

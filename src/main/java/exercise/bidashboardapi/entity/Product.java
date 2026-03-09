package exercise.bidashboardapi.entity;

import exercise.bidashboardapi.validation.NoSpecialCharacters;
import exercise.bidashboardapi.validation.SafeString;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    @SafeString(message = "Product name contains potentially dangerous characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    @NoSpecialCharacters(message = "Category can only contain letters, numbers, spaces, hyphens and underscores")
    @Column(name = "category", length = 50)
    private String category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Price must not exceed 999,999.99")
    @Column(name = "price", nullable = false)
    private Double price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    @Max(value = 999999, message = "Stock must not exceed 999,999")
    @Column(name = "stock", nullable = false)
    private Integer stock;
}
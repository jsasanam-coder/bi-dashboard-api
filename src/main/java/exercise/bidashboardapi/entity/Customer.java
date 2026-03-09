package exercise.bidashboardapi.entity;

import exercise.bidashboardapi.validation.NoSpecialCharacters;
import exercise.bidashboardapi.validation.SafeString;
import exercise.bidashboardapi.validation.ValidEmail;
import exercise.bidashboardapi.validation.ValidPhoneNumber;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer customerId;

    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @NoSpecialCharacters(message = "Name can only contain letters, numbers, spaces, hyphens and underscores")
    @SafeString(message = "Name contains potentially dangerous characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @ValidEmail(message = "Please provide a valid email address")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @ValidPhoneNumber(message = "Please provide a valid phone number")
    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Size(max = 200, message = "Address must not exceed 200 characters")
    @SafeString(message = "Address contains potentially dangerous characters")
    @Column(name = "address", length = 200)
    private String address;

    @Size(max = 50, message = "City must not exceed 50 characters")
    @NoSpecialCharacters(message = "City can only contain letters, numbers, spaces, hyphens and underscores")
    @Column(name = "city", length = 50)
    private String city;

    @Size(max = 50, message = "Country must not exceed 50 characters")
    @NoSpecialCharacters(message = "Country can only contain letters, numbers, spaces, hyphens and underscores")
    @Column(name = "country", length = 50)
    private String country;
}
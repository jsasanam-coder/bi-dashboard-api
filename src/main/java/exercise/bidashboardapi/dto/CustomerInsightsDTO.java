package exercise.bidashboardapi.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerInsightsDTO {
    // From SQL Server (current profile)
    private Integer customerId;
    private String name;
    private String email;
    private String city;
    private String country;

    // From Snowflake (analytics)
    private Long lifetimeOrders;
    private Double lifetimeValue;
    private Double averageOrderValue;
    private String segment;  // VIP, REGULAR, NEW
    private String favoriteCategory;
}

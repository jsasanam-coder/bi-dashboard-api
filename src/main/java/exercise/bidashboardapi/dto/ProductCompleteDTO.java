package exercise.bidashboardapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCompleteDTO {
    // From SQL Server (current data)
    private Integer productId;
    private String name;
    private String category;
    private Double currentPrice;
    private Integer currentStock;

    // From Snowflake (historical analytics)
    private Long totalUnitsSold;
    private Double totalRevenue;
    private Double averageSellingPrice;
    private Integer daysInCatalog;
}

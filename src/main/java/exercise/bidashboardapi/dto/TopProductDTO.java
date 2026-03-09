package exercise.bidashboardapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductDTO {
    private String productName;
    private String category;
    private Long totalUnitsSold;
    private Double totalRevenue;
    private Integer currentStock;
}

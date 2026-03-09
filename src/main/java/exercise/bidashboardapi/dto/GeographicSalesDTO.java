package exercise.bidashboardapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeographicSalesDTO {
    private String country;
    private String city;
    private Long orderCount;
    private Double totalRevenue;
    private Long customerCount;
}

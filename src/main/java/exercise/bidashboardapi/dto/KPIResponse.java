package exercise.bidashboardapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KPIResponse {
    private Double totalRevenue;
    private Long totalOrders;
    private Double averageOrderValue;
    private Long totalCustomers;
    private Long totalProducts;
}

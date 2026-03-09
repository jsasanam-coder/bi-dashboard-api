package exercise.bidashboardapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSegmentDTO {
    private String segment;  // VIP, REGULAR, NEW
    private Long customerCount;
    private Double totalRevenue;
    private Double averageSpending;
}

package exercise.bidashboardapi.controller;

import exercise.bidashboardapi.dto.CustomerInsightsDTO;
import exercise.bidashboardapi.dto.ProductCompleteDTO;
import exercise.bidashboardapi.service.HybridService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportsController {

    private final HybridService hybridService;

    @GetMapping("/product/{id}/complete")
    public ProductCompleteDTO getCompleteProductInfo(@PathVariable Integer id) {
        log.info("Entering ReportsController.getCompleteProductInfo with productId={}", id);
        ProductCompleteDTO product = hybridService.getCompleteProductInfo(id);
        log.info("Exiting ReportsController.getCompleteProductInfo with product name={}", product.getName());
        return product;
    }

    @GetMapping("/customer/{id}/insights")
    public CustomerInsightsDTO getCustomerInsights(@PathVariable Integer id) {
        log.info("Entering ReportsController.getCustomerInsights with customerId={}", id);
        CustomerInsightsDTO insights = hybridService.getCompleteCustomerInsights(id);
        log.info("Exiting ReportsController.getCustomerInsights with customer name={}", insights.getName());
        return insights;
    }
}

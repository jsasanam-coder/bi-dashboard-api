package exercise.bidashboardapi.controller;

import exercise.bidashboardapi.dto.*;
import exercise.bidashboardapi.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final AnalyticsService analyticsService;

    @GetMapping("/kpis")
    public KPIResponse getKPIs() {
        log.info("Entering DashboardController.getKPIs");
        KPIResponse response = analyticsService.getKPIs();
        log.info("Exiting DashboardController.getKPIs with KPI data");
        return response;
    }

    @GetMapping("/sales-trends")
    public List<SalesTrendDTO> getSalesTrends(
            @RequestParam(defaultValue = "daily") String period
    ) {
        log.info("Entering DashboardController.getSalesTrends with period={}", period);
        List<SalesTrendDTO> trends = analyticsService.getSalesTrends(period);
        log.info("Exiting DashboardController.getSalesTrends with {} trends", trends.size());
        return trends;
    }

    @GetMapping("/top-products")
    public List<TopProductDTO> getTopProducts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("Entering DashboardController.getTopProducts with category={}, limit={}", category, limit);
        List<TopProductDTO> products = analyticsService.getTopProducts(category, limit);
        log.info("Exiting DashboardController.getTopProducts with {} products", products.size());
        return products;
    }

    @GetMapping("/customer-segments")
    public List<CustomerSegmentDTO> getCustomerSegments() {
        log.info("Entering DashboardController.getCustomerSegments");
        List<CustomerSegmentDTO> segments = analyticsService.getCustomerSegments();
        log.info("Exiting DashboardController.getCustomerSegments with {} segments", segments.size());
        return segments;
    }

    @GetMapping("/geographic-sales")
    public List<GeographicSalesDTO> getGeographicSales() {
        log.info("Entering DashboardController.getGeographicSales");
        List<GeographicSalesDTO> sales = analyticsService.getGeographicSales();
        log.info("Exiting DashboardController.getGeographicSales with {} regions", sales.size());
        return sales;
    }
}

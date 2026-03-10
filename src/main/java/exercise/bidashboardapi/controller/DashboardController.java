package exercise.bidashboardapi.controller;

import exercise.bidashboardapi.dto.*;
import exercise.bidashboardapi.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard Analytics", description = "Dashboard KPIs and Analytics APIs")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Get KPIs", description = "Retrieves key performance indicators including total sales, orders, and customers")
    @ApiResponse(responseCode = "200", description = "KPIs retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    @GetMapping("/kpis")
    public KPIResponse getKPIs() {
        log.info("Entering DashboardController.getKPIs");
        KPIResponse response = analyticsService.getKPIs();
        log.info("Exiting DashboardController.getKPIs with KPI data");
        return response;
    }

    @Operation(summary = "Get sales trends", description = "Retrieves sales trends data grouped by period (daily, weekly, monthly)")
    @ApiResponse(responseCode = "200", description = "Sales trends retrieved successfully")
    @GetMapping("/sales-trends")
    public List<SalesTrendDTO> getSalesTrends(
            @Parameter(description = "Time period for grouping: daily, weekly, or monthly")
            @RequestParam(defaultValue = "daily") String period
    ) {
        log.info("Entering DashboardController.getSalesTrends with period={}", period);
        List<SalesTrendDTO> trends = analyticsService.getSalesTrends(period);
        log.info("Exiting DashboardController.getSalesTrends with {} trends", trends.size());
        return trends;
    }

    @Operation(summary = "Get top products", description = "Retrieves top selling products, optionally filtered by category")
    @ApiResponse(responseCode = "200", description = "Top products retrieved successfully")
    @GetMapping("/top-products")
    public List<TopProductDTO> getTopProducts(
            @Parameter(description = "Filter by product category (optional)")
            @RequestParam(required = false) String category,
            @Parameter(description = "Maximum number of products to return")
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("Entering DashboardController.getTopProducts with category={}, limit={}", category, limit);
        List<TopProductDTO> products = analyticsService.getTopProducts(category, limit);
        log.info("Exiting DashboardController.getTopProducts with {} products", products.size());
        return products;
    }

    @Operation(summary = "Get customer segments", description = "Retrieves customer segmentation data for analytics")
    @ApiResponse(responseCode = "200", description = "Customer segments retrieved successfully")
    @GetMapping("/customer-segments")
    public List<CustomerSegmentDTO> getCustomerSegments() {
        log.info("Entering DashboardController.getCustomerSegments");
        List<CustomerSegmentDTO> segments = analyticsService.getCustomerSegments();
        log.info("Exiting DashboardController.getCustomerSegments with {} segments", segments.size());
        return segments;
    }

    @Operation(summary = "Get geographic sales", description = "Retrieves sales data grouped by geographic region")
    @ApiResponse(responseCode = "200", description = "Geographic sales data retrieved successfully")
    @GetMapping("/geographic-sales")
    public List<GeographicSalesDTO> getGeographicSales() {
        log.info("Entering DashboardController.getGeographicSales");
        List<GeographicSalesDTO> sales = analyticsService.getGeographicSales();
        log.info("Exiting DashboardController.getGeographicSales with {} regions", sales.size());
        return sales;
    }
}

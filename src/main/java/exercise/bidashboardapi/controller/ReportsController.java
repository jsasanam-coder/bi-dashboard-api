package exercise.bidashboardapi.controller;

import exercise.bidashboardapi.dto.CustomerInsightsDTO;
import exercise.bidashboardapi.dto.ProductCompleteDTO;
import exercise.bidashboardapi.service.HybridService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "Hybrid Reports APIs - combines data from MSSQL and Snowflake")
@SecurityRequirement(name = "bearerAuth")
public class ReportsController {

    private final HybridService hybridService;

    @Operation(summary = "Get complete product info", description = "Retrieves complete product information combining MSSQL and Snowflake data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product info retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ProductCompleteDTO.class))),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/product/{id}/complete")
    public ProductCompleteDTO getCompleteProductInfo(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Integer id) {
        log.info("Entering ReportsController.getCompleteProductInfo with productId={}", id);
        ProductCompleteDTO product = hybridService.getCompleteProductInfo(id);
        log.info("Exiting ReportsController.getCompleteProductInfo with product name={}", product.getName());
        return product;
    }

    @Operation(summary = "Get customer insights", description = "Retrieves complete customer insights combining MSSQL and Snowflake data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer insights retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CustomerInsightsDTO.class))),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/customer/{id}/insights")
    public CustomerInsightsDTO getCustomerInsights(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable Integer id) {
        log.info("Entering ReportsController.getCustomerInsights with customerId={}", id);
        CustomerInsightsDTO insights = hybridService.getCompleteCustomerInsights(id);
        log.info("Exiting ReportsController.getCustomerInsights with customer name={}", insights.getName());
        return insights;
    }
}

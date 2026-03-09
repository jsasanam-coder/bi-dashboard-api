package exercise.bidashboardapi.service;

import exercise.bidashboardapi.dto.CustomerInsightsDTO;
import exercise.bidashboardapi.dto.ProductCompleteDTO;
import exercise.bidashboardapi.entity.Customer;
import exercise.bidashboardapi.entity.Product;
import exercise.bidashboardapi.exception.BadRequestException;
import exercise.bidashboardapi.exception.InternalServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class HybridService {

    private final TransactionalService transactionalService;
    private final AnalyticsService analyticsService;

    public ProductCompleteDTO getCompleteProductInfo(Integer productId) {
        log.info("Fetching complete product info for: {}", productId);

        // Validation
        if (productId == null) {
            throw new BadRequestException("Product ID cannot be null");
        }
        if (productId <= 0) {
            throw new BadRequestException("Product ID must be a positive number");
        }

        long startTime = System.currentTimeMillis();

        try {
            // Parallel execution - query both databases at same time
            CompletableFuture<Product> productFuture = CompletableFuture.supplyAsync(() ->
                    transactionalService.getProductById(productId)
            );

            CompletableFuture<Map<String, Object>> analyticsFuture = CompletableFuture.supplyAsync(() ->
                    analyticsService.getProductAnalytics(productId)
            );

            // Wait for both to complete
            Product product = productFuture.join();
            Map<String, Object> analytics = analyticsFuture.join();

            long endTime = System.currentTimeMillis();
            log.info("Complete product info fetched for productId={}, name={} in {} ms",
                    productId, product.getName(), (endTime - startTime));

            // Combine results
            return ProductCompleteDTO.builder()
                    .productId(product.getProductId())
                    .name(product.getName())
                    .category(product.getCategory())
                    .currentPrice(product.getPrice())
                    .currentStock(product.getStock())
                    .totalUnitsSold(analytics.get("TOTAL_UNITS_SOLD") != null
                            ? ((Number) analytics.get("TOTAL_UNITS_SOLD")).longValue()
                            : 0L)
                    .totalRevenue((Double) analytics.get("TOTAL_REVENUE"))
                    .averageSellingPrice((Double) analytics.get("AVERAGE_SELLING_PRICE"))
                    .daysInCatalog(0)
                    .build();
        } catch (Exception ex) {
            log.error("Error fetching complete product info: {}", ex.getMessage());
            throw new InternalServerException("Failed to fetch complete product information", ex);
        }
    }

    public CustomerInsightsDTO getCompleteCustomerInsights(Integer customerId) {
        log.info("Fetching complete customer insights for: {}", customerId);

        // Validation
        if (customerId == null) {
            throw new BadRequestException("Customer ID cannot be null");
        }
        if (customerId <= 0) {
            throw new BadRequestException("Customer ID must be a positive number");
        }

        long startTime = System.currentTimeMillis();

        try {
            // Parallel execution
            CompletableFuture<Customer> customerFuture = CompletableFuture.supplyAsync(() ->
                    transactionalService.getCustomerById(customerId)
            );

            CompletableFuture<Map<String, Object>> analyticsFuture = CompletableFuture.supplyAsync(() ->
                    analyticsService.getCustomerAnalytics(customerId)
            );

            // Wait for both
            Customer customer = customerFuture.join();
            Map<String, Object> analytics = analyticsFuture.join();

            long endTime = System.currentTimeMillis();
            log.info("Complete customer insights fetched for customerId={}, name={} in {} ms",
                    customerId, customer.getName(), (endTime - startTime));

            // Combine results
            return CustomerInsightsDTO.builder()
                    .customerId(customer.getCustomerId())
                    .name(customer.getName())
                    .email(customer.getEmail())
                    .city(customer.getCity())
                    .country(customer.getCountry())
                    .lifetimeOrders(analytics.get("LIFETIME_ORDERS") != null
                            ? ((Number) analytics.get("LIFETIME_ORDERS")).longValue()
                            : 0L)
                    .lifetimeValue((Double) analytics.get("LIFETIME_VALUE"))
                    .averageOrderValue((Double) analytics.get("AVERAGE_ORDER_VALUE"))
                    .segment((String) analytics.get("SEGMENT"))
                    .favoriteCategory("Electronics")
                    .build();
        } catch (Exception ex) {
            log.error("Error fetching complete customer insights: {}", ex.getMessage());
            throw new InternalServerException("Failed to fetch complete customer insights", ex);
        }
    }
}
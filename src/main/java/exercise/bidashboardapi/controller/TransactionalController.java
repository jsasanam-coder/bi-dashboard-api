package exercise.bidashboardapi.controller;

import exercise.bidashboardapi.entity.*;
import exercise.bidashboardapi.service.TransactionalService;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactional Operations", description = "APIs for Orders, Customers, and Product Stock Management")
@SecurityRequirement(name = "bearerAuth")
public class TransactionalController {

    private final TransactionalService transactionalService;

    @Operation(summary = "Create new order", description = "Creates a new order with order items and updates product stock")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "400", description = "Invalid order data or insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Customer or product not found")
    })
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@RequestBody OrderRequest request) {
        log.info("Entering TransactionalController.createOrder with customerId={}, items count={}",
                request.getOrder().getCustomerId(), request.getItems().size());
        Order order = transactionalService.createOrder(request.getOrder(), request.getItems());
        log.info("Exiting TransactionalController.createOrder with orderId={}", order.getOrderId());
        return order;
    }

    @Operation(summary = "Get order by ID", description = "Retrieves order details by order ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "400", description = "Invalid order ID"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/orders/{id}")
    public Order getOrder(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Integer id) {
        log.info("Entering TransactionalController.getOrder with orderId={}", id);
        Order order = transactionalService.getOrderById(id);
        log.info("Exiting TransactionalController.getOrder with order status={}", order.getStatus());
        return order;
    }

    @Operation(summary = "Get customer by ID", description = "Retrieves customer details by customer ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer found",
                    content = @Content(schema = @Schema(implementation = Customer.class))),
            @ApiResponse(responseCode = "400", description = "Invalid customer ID"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/customers/{id}")
    public Customer getCustomer(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable Integer id) {
        log.info("Entering TransactionalController.getCustomer with customerId={}", id);
        Customer customer = transactionalService.getCustomerById(id);
        log.info("Exiting TransactionalController.getCustomer with customer name={}", customer.getName());
        return customer;
    }

    @Operation(summary = "Update product stock", description = "Updates the stock level of a product (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock updated successfully",
                    content = @Content(schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode = "400", description = "Invalid product ID or negative stock value"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/products/{id}/stock")
    public Product updateStock(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Integer id,
            @Parameter(description = "New stock quantity (must be non-negative)", required = true)
            @RequestParam Integer stock) {
        log.info("Entering TransactionalController.updateStock with productId={}, newStock={}", id, stock);
        Product product = transactionalService.updateStock(id, stock);
        log.info("Exiting TransactionalController.updateStock with updated stock={}", product.getStock());
        return product;
    }

    @Schema(description = "Order creation request containing order details and items")
    @lombok.Data
    public static class OrderRequest {
        @Schema(description = "Order header information")
        private Order order;
        @Schema(description = "List of order items")
        private List<OrderItem> items;
    }
}

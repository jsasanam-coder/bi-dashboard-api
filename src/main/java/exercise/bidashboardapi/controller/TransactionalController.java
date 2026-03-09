package exercise.bidashboardapi.controller;

import exercise.bidashboardapi.entity.*;
import exercise.bidashboardapi.service.TransactionalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class TransactionalController {

    private final TransactionalService transactionalService;

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@RequestBody OrderRequest request) {
        log.info("Entering TransactionalController.createOrder with customerId={}, items count={}",
                request.getOrder().getCustomerId(), request.getItems().size());
        Order order = transactionalService.createOrder(request.getOrder(), request.getItems());
        log.info("Exiting TransactionalController.createOrder with orderId={}", order.getOrderId());
        return order;
    }

    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable Integer id) {
        log.info("Entering TransactionalController.getOrder with orderId={}", id);
        Order order = transactionalService.getOrderById(id);
        log.info("Exiting TransactionalController.getOrder with order status={}", order.getStatus());
        return order;
    }

    @GetMapping("/customers/{id}")
    public Customer getCustomer(@PathVariable Integer id) {
        log.info("Entering TransactionalController.getCustomer with customerId={}", id);
        Customer customer = transactionalService.getCustomerById(id);
        log.info("Exiting TransactionalController.getCustomer with customer name={}", customer.getName());
        return customer;
    }

    @PutMapping("/products/{id}/stock")
    public Product updateStock(@PathVariable Integer id, @RequestParam Integer stock) {
        log.info("Entering TransactionalController.updateStock with productId={}, newStock={}", id, stock);
        Product product = transactionalService.updateStock(id, stock);
        log.info("Exiting TransactionalController.updateStock with updated stock={}", product.getStock());
        return product;
    }

    // Inner class for request body
    @lombok.Data
    public static class OrderRequest {
        private Order order;
        private List<OrderItem> items;
    }
}

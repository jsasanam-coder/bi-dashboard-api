package exercise.bidashboardapi.service;

import exercise.bidashboardapi.entity.Customer;
import exercise.bidashboardapi.entity.Order;
import exercise.bidashboardapi.entity.OrderItem;
import exercise.bidashboardapi.entity.Product;
import exercise.bidashboardapi.exception.BadRequestException;
import exercise.bidashboardapi.exception.ResourceNotFoundException;
import exercise.bidashboardapi.repository.CustomerRepository;
import exercise.bidashboardapi.repository.OrderItemRepository;
import exercise.bidashboardapi.repository.OrderRepository;
import exercise.bidashboardapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionalService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Order createOrder(Order order, List<OrderItem> items) {
        long startTime = System.currentTimeMillis();
        log.info("Creating order for customerId={}, totalAmount={}, items count={}",
                order.getCustomerId(), order.getTotalAmount(), items != null ? items.size() : 0);

        // Validate customer exists
        if (!customerRepository.existsById(order.getCustomerId())) {
            throw new ResourceNotFoundException("Customer", "id", order.getCustomerId());
        }

        // Validate order amount is positive
        if (order.getTotalAmount() != null && order.getTotalAmount() <= 0) {
            throw new BadRequestException("Order total amount must be positive");
        }

        // Validate items list is not empty
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }

        // Set order date
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");

        // Save order
        Order savedOrder = orderRepository.save(order);
        log.debug("Order entity saved with orderId={}", savedOrder.getOrderId());

        // Save order items
        for (OrderItem item : items) {
            // Validate product exists
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", item.getProductId()));

            // Validate stock availability
            if (product.getStock() < item.getQuantity()) {
                throw new BadRequestException(
                        String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                                product.getName(), product.getStock(), item.getQuantity())
                );
            }

            item.setOrderId(savedOrder.getOrderId());
            orderItemRepository.save(item);

            // Update stock
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Order created successfully with orderId={}, status={}, {} items processed in {}ms",
                savedOrder.getOrderId(), savedOrder.getStatus(), items.size(), duration);
        return savedOrder;
    }

    public Order getOrderById(Integer orderId) {
        log.info("Fetching order with orderId={}", orderId);

        // Validate orderId is positive
        if (orderId <= 0) {
            throw new BadRequestException("Order ID must be a positive number");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        log.info("Fetched order orderId={}, status={}, totalAmount={}",
                order.getOrderId(), order.getStatus(), order.getTotalAmount());
        return order;
    }

    public Customer getCustomerById(Integer customerId) {
        log.info("Fetching customer with customerId={}", customerId);

        // Validate customerId is positive
        if (customerId <= 0) {
            throw new BadRequestException("Customer ID must be a positive number");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        log.info("Fetched customer customerId={}, name={}", customer.getCustomerId(), customer.getName());
        return customer;
    }

    @Transactional
    public Product updateStock(Integer productId, Integer newStock) {
        log.info("Updating stock for productId={} from current to new={}", productId, newStock);

        // Validate productId is positive
        if (productId <= 0) {
            throw new BadRequestException("Product ID must be a positive number");
        }

        // Validate stock is non-negative
        if (newStock < 0) {
            throw new BadRequestException("Stock cannot be negative");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        Integer oldStock = product.getStock();
        product.setStock(newStock);
        Product updatedProduct = productRepository.save(product);

        log.info("Stock updated for productId={}, previousStock={}, newStock={}",
                productId, oldStock, updatedProduct.getStock());
        return updatedProduct;
    }

    public Product getProductById(Integer productId) {
        log.info("Fetching product with productId={}", productId);

        // Validate productId is positive
        if (productId <= 0) {
            throw new BadRequestException("Product ID must be a positive number");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        log.info("Fetched product productId={}, name={}, stock={}",
                product.getProductId(), product.getName(), product.getStock());
        return product;
    }
}
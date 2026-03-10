package exercise.bidashboardapi.service;

import exercise.bidashboardapi.entity.*;
import exercise.bidashboardapi.exception.BadRequestException;
import exercise.bidashboardapi.exception.ResourceNotFoundException;
import exercise.bidashboardapi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionalService
 * Tests business logic in isolation using mocks
 */
@ExtendWith(MockitoExtension.class)  // Enable Mockito
@DisplayName("TransactionalService Tests")
class TransactionalServiceTest {

    // Mocks (fake dependencies)
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    // Object under test (real service with mocked dependencies)
    @InjectMocks
    private TransactionalService transactionalService;

    // Test data
    private Customer testCustomer;
    private Product testProduct;
    private Order testOrder;
    private OrderItem testOrderItem;

    /**
     * Setup test data before each test
     */
    @BeforeEach
    void setUp() {
        // Create test customer
        testCustomer = new Customer();
        testCustomer.setCustomerId(1);
        testCustomer.setName("John Doe");
        testCustomer.setEmail("john@example.com");

        // Create test product
        testProduct = new Product();
        testProduct.setProductId(1);
        testProduct.setName("Laptop");
        testProduct.setCategory("Electronics");
        testProduct.setPrice(999.99);
        testProduct.setStock(10);

        // Create test order
        testOrder = new Order();
        testOrder.setCustomerId(1);
        testOrder.setTotalAmount(999.99);

        // Create test order item
        testOrderItem = new OrderItem();
        testOrderItem.setProductId(1);
        testOrderItem.setQuantity(1);
        testOrderItem.setPrice(999.99);
    }

    // ==================== CREATE ORDER TESTS ====================

    @Test
    @DisplayName("Should create order successfully")
    void testCreateOrder_Success() {
        // Arrange (setup)
        List<OrderItem> items = Arrays.asList(testOrderItem);
        Order savedOrder = new Order();
        savedOrder.setOrderId(1);
        savedOrder.setCustomerId(1);
        savedOrder.setTotalAmount(999.99);
        savedOrder.setStatus("PENDING");
        savedOrder.setOrderDate(LocalDateTime.now());

        // Mock behavior
        when(customerRepository.existsById(1)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act (execute)
        Order result = transactionalService.createOrder(testOrder, items);

        // Assert (verify)
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getOrderDate()).isNotNull();

        // Verify interactions
        verify(customerRepository, times(1)).existsById(1);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).save(testOrderItem);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void testCreateOrder_CustomerNotFound_ThrowsException() {
        // Arrange
        List<OrderItem> items = Arrays.asList(testOrderItem);

        // Mock customer does not exist
        when(customerRepository.existsById(1)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> transactionalService.createOrder(testOrder, items))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");

        // Verify repository was never called
        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when order amount is negative")
    void testCreateOrder_NegativeAmount_ThrowsException() {
        // Arrange
        testOrder.setTotalAmount(-100.0);
        List<OrderItem> items = Arrays.asList(testOrderItem);

        when(customerRepository.existsById(1)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> transactionalService.createOrder(testOrder, items))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("positive");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when items list is empty")
    void testCreateOrder_EmptyItems_ThrowsException() {
        // Arrange
        List<OrderItem> items = Collections.emptyList();

        when(customerRepository.existsById(1)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> transactionalService.createOrder(testOrder, items))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one item");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when insufficient stock")
    void testCreateOrder_InsufficientStock_ThrowsException() {
        // Arrange
        testOrderItem.setQuantity(100); // More than available stock (10)
        List<OrderItem> items = Arrays.asList(testOrderItem);
        Order savedOrder = new Order();
        savedOrder.setOrderId(1);

        when(customerRepository.existsById(1)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));

        // Act & Assert
        assertThatThrownBy(() -> transactionalService.createOrder(testOrder, items))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
    }

    // ==================== GET CUSTOMER TESTS ====================

    @Test
    @DisplayName("Should get customer by ID successfully")
    void testGetCustomerById_Success() {
        // Arrange
        when(customerRepository.findById(1)).thenReturn(Optional.of(testCustomer));

        // Act
        Customer result = transactionalService.getCustomerById(1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(1);
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");

        verify(customerRepository).findById(1);
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void testGetCustomerById_NotFound_ThrowsException() {
        // Arrange
        when(customerRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionalService.getCustomerById(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer")
                .hasMessageContaining("999");

        verify(customerRepository).findById(999);
    }

    @Test
    @DisplayName("Should throw exception for invalid customer ID")
    void testGetCustomerById_InvalidId_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> transactionalService.getCustomerById(-1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Customer ID must be a positive number");

        verify(customerRepository, never()).findById(any());
    }

    // ==================== GET PRODUCT TESTS ====================

    @Test
    @DisplayName("Should get product by ID successfully")
    void testGetProductById_Success() {
        // Arrange
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));

        // Act
        Product result = transactionalService.getProductById(1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1);
        assertThat(result.getName()).isEqualTo("Laptop");
        assertThat(result.getPrice()).isEqualTo(999.99);
        assertThat(result.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void testGetProductById_NotFound_ThrowsException() {
        // Arrange
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionalService.getProductById(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining("999");
    }

    // ==================== UPDATE STOCK TESTS ====================

    @Test
    @DisplayName("Should update stock successfully")
    void testUpdateStock_Success() {
        // Arrange
        Product updatedProduct = new Product();
        updatedProduct.setProductId(1);
        updatedProduct.setStock(20);

        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        // Act
        Product result = transactionalService.updateStock(1, 20);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStock()).isEqualTo(20);

        verify(productRepository).findById(1);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception for negative stock")
    void testUpdateStock_NegativeStock_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> transactionalService.updateStock(1, -5))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Stock cannot be negative");

        verify(productRepository, never()).save(any());
    }

    // ==================== GET ORDER TESTS ====================

    @Test
    @DisplayName("Should get order by ID successfully")
    void testGetOrderById_Success() {
        // Arrange
        Order order = new Order();
        order.setOrderId(1);
        order.setCustomerId(1);
        order.setTotalAmount(999.99);

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        // Act
        Order result = transactionalService.getOrderById(1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1);
        assertThat(result.getTotalAmount()).isEqualTo(999.99);
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void testGetOrderById_NotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionalService.getOrderById(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order")
                .hasMessageContaining("999");
    }
}

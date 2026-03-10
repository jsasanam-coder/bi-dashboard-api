package exercise.bidashboardapi.controller;

import exercise.bidashboardapi.entity.Customer;
import exercise.bidashboardapi.entity.Order;
import exercise.bidashboardapi.entity.OrderItem;
import exercise.bidashboardapi.entity.Product;
import exercise.bidashboardapi.exception.BadRequestException;
import exercise.bidashboardapi.exception.ResourceNotFoundException;
import exercise.bidashboardapi.security.JwtAuthenticationEntryPoint;
import exercise.bidashboardapi.security.JwtAuthenticationFilter;
import exercise.bidashboardapi.service.CustomUserDetailsService;
import exercise.bidashboardapi.util.JwtUtils;
import exercise.bidashboardapi.service.TransactionalService;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for TransactionalController
 */
@WebMvcTest(TransactionalController.class)
@AutoConfigureMockMvc
@DisplayName("TransactionalController Tests")
class TransactionalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = JsonMapper.builder().build();

    @MockitoBean
    private TransactionalService transactionalService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private Customer testCustomer;
    private Product testProduct;
    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setCustomerId(1);
        testCustomer.setName("John Doe");
        testCustomer.setEmail("john@example.com");
        testCustomer.setPhoneNumber("1234567890");
        testCustomer.setCity("New York");
        testCustomer.setCountry("USA");

        testProduct = new Product();
        testProduct.setProductId(1);
        testProduct.setName("Laptop");
        testProduct.setCategory("Electronics");
        testProduct.setPrice(999.99);
        testProduct.setStock(10);

        testOrder = new Order();
        testOrder.setOrderId(1);
        testOrder.setCustomerId(1);
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setTotalAmount(999.99);
        testOrder.setStatus("PENDING");

        testOrderItem = new OrderItem();
        testOrderItem.setOrderItemId(1);
        testOrderItem.setOrderId(1);
        testOrderItem.setProductId(1);
        testOrderItem.setQuantity(1);
        testOrderItem.setPrice(999.99);
    }

    // ==================== CREATE ORDER TESTS ====================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/orders - Success")
    void testCreateOrder_Success() throws Exception {
        // Arrange
        List<OrderItem> items = Arrays.asList(testOrderItem);
        TransactionalController.OrderRequest request = new TransactionalController.OrderRequest();
        request.setOrder(testOrder);
        request.setItems(items);

        when(transactionalService.createOrder(any(Order.class), anyList()))
                .thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.totalAmount").value(999.99))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(transactionalService, times(1)).createOrder(any(Order.class), anyList());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/orders - Invalid Order Data")
    void testCreateOrder_InvalidData_ReturnsBadRequest() throws Exception {
        // Arrange
        testOrder.setTotalAmount(-100.0);  // Invalid amount
        List<OrderItem> items = Arrays.asList(testOrderItem);
        TransactionalController.OrderRequest request = new TransactionalController.OrderRequest();
        request.setOrder(testOrder);
        request.setItems(items);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/orders - Service Throws Exception")
    void testCreateOrder_ServiceException_ReturnsBadRequest() throws Exception {
        // Arrange
        List<OrderItem> items = Arrays.asList(testOrderItem);
        TransactionalController.OrderRequest request = new TransactionalController.OrderRequest();
        request.setOrder(testOrder);
        request.setItems(items);

        when(transactionalService.createOrder(any(Order.class), anyList()))
                .thenThrow(new BadRequestException("Insufficient stock"));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient stock"));
    }

    // ==================== GET ORDER TESTS ====================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/orders/{id} - Success")
    void testGetOrder_Success() throws Exception {
        // Arrange
        when(transactionalService.getOrderById(1)).thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.totalAmount").value(999.99))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(transactionalService, times(1)).getOrderById(1);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/orders/{id} - Not Found")
    void testGetOrder_NotFound_ReturnsNotFound() throws Exception {
        // Arrange
        when(transactionalService.getOrderById(999))
                .thenThrow(new ResourceNotFoundException("Order", "id", 999));

        // Act & Assert
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Order")))
                .andExpect(jsonPath("$.message").value(containsString("999")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/orders/{id} - Invalid ID Type")
    void testGetOrder_InvalidIdType_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/orders/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("type mismatch")));
    }

    // ==================== GET CUSTOMER TESTS ====================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/customers/{id} - Success")
    void testGetCustomer_Success() throws Exception {
        // Arrange
        when(transactionalService.getCustomerById(1)).thenReturn(testCustomer);

        // Act & Assert
        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.city").value("New York"));

        verify(transactionalService, times(1)).getCustomerById(1);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/customers/{id} - Not Found")
    void testGetCustomer_NotFound_ReturnsNotFound() throws Exception {
        // Arrange
        when(transactionalService.getCustomerById(999))
                .thenThrow(new ResourceNotFoundException("Customer", "id", 999));

        // Act & Assert
        mockMvc.perform(get("/api/customers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/customers/{id} - Negative ID")
    void testGetCustomer_NegativeId_ReturnsBadRequest() throws Exception {
        // Arrange
        when(transactionalService.getCustomerById(-1))
                .thenThrow(new BadRequestException("Customer ID must be a positive number"));

        // Act & Assert
        mockMvc.perform(get("/api/customers/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("positive")));
    }

    // ==================== UPDATE STOCK TESTS ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/products/{id}/stock - Success")
    void testUpdateStock_Success() throws Exception {
        // Arrange
        testProduct.setStock(20);
        when(transactionalService.updateStock(1, 20)).thenReturn(testProduct);

        // Act & Assert
        mockMvc.perform(put("/api/products/1/stock")
                        .with(csrf())
                        .param("stock", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.stock").value(20));

        verify(transactionalService, times(1)).updateStock(1, 20);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/products/{id}/stock - Negative Stock")
    void testUpdateStock_NegativeStock_ReturnsBadRequest() throws Exception {
        // Arrange
        when(transactionalService.updateStock(1, -5))
                .thenThrow(new BadRequestException("Stock cannot be negative"));

        // Act & Assert
        mockMvc.perform(put("/api/products/1/stock")
                        .with(csrf())
                        .param("stock", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Stock cannot be negative"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/products/{id}/stock - Product Not Found")
    void testUpdateStock_ProductNotFound_ReturnsNotFound() throws Exception {
        // Arrange
        when(transactionalService.updateStock(999, 10))
                .thenThrow(new ResourceNotFoundException("Product", "id", 999));

        // Act & Assert
        mockMvc.perform(put("/api/products/999/stock")
                        .with(csrf())
                        .param("stock", "10"))
                .andExpect(status().isNotFound());
    }

    // ==================== AUTHORIZATION TESTS ====================

    @Test
    @DisplayName("GET /api/orders/{id} - No Authentication")
    void testGetOrder_NoAuth_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isUnauthorized());

        verify(transactionalService, never()).getOrderById(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("PUT /api/products/{id}/stock - Wrong Role")
    void testUpdateStock_WrongRole_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/products/1/stock")
                        .with(csrf())
                        .param("stock", "20"))
                .andExpect(status().isForbidden());

        verify(transactionalService, never()).updateStock(any(), any());
    }
}

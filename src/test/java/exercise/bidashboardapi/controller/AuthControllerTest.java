package exercise.bidashboardapi.controller;

import exercise.bidashboardapi.dto.auth.*;
import exercise.bidashboardapi.exception.BadRequestException;
import exercise.bidashboardapi.exception.ConflictException;
import exercise.bidashboardapi.security.JwtAuthenticationEntryPoint;
import exercise.bidashboardapi.security.JwtAuthenticationFilter;
import exercise.bidashboardapi.service.CustomUserDetailsService;
import exercise.bidashboardapi.util.JwtUtils;
import exercise.bidashboardapi.service.AuthService;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for AuthController
 * Uses MockMvc to test HTTP requests/responses
 */
@WebMvcTest(AuthController.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = JsonMapper.builder().build();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("Test@123");
        registerRequest.setRole("USER");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Test@123");

        authResponse = AuthResponse.builder()
                .accessToken("access-token-123")
                .refreshToken("refresh-token-456")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .build();
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("POST /api/auth/register - Success")
    void testRegister_Success() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-456"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Missing Username")
    void testRegister_MissingUsername_ReturnsBadRequest() throws Exception {
        // Arrange
        registerRequest.setUsername("");  // Invalid

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /api/auth/register - Invalid Email")
    void testRegister_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        registerRequest.setEmail("not-an-email");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value(containsString("Email")));

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /api/auth/register - Weak Password")
    void testRegister_WeakPassword_ReturnsBadRequest() throws Exception {
        // Arrange
        registerRequest.setPassword("weak");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value(containsString("Password")));

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /api/auth/register - Duplicate Username")
    void testRegister_DuplicateUsername_ReturnsConflict() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ConflictException("User", "username", "testuser"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("username")))
                .andExpect(jsonPath("$.message").value(containsString("testuser")));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("POST /api/auth/login - Success")
    void testLogin_Success() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - Wrong Password")
    void testLogin_WrongPassword_ReturnsUnauthorized() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - Missing Fields")
    void testLogin_MissingFields_ReturnsBadRequest() throws Exception {
        // Arrange
        loginRequest.setUsername("");
        loginRequest.setPassword("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value(containsString("Username")))
                .andExpect(jsonPath("$.details").value(containsString("Password")));

        verify(authService, never()).login(any());
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Test
    @DisplayName("POST /api/auth/refresh - Success")
    void testRefreshToken_Success() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"));

        verify(authService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Invalid Token")
    void testRefreshToken_InvalidToken_ReturnsBadRequest() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new BadRequestException("Invalid or expired refresh token"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }
}

package exercise.bidashboardapi.service;

import exercise.bidashboardapi.dto.auth.*;
import exercise.bidashboardapi.entity.User;
import exercise.bidashboardapi.exception.BadRequestException;
import exercise.bidashboardapi.exception.ConflictException;
import exercise.bidashboardapi.exception.ResourceNotFoundException;
import exercise.bidashboardapi.repository.UserRepository;
import exercise.bidashboardapi.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Setup register request
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("Test@123");
        registerRequest.setRole("USER");

        // Setup login request
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Test@123");

        // Setup test user
        testUser = User.builder()
                .userId(1)
                .username("testuser")
                .email("test@example.com")
                .password("$2a$10$encodedPassword")
                .role("USER")
                .enabled(true)
                .build();
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("Should register user successfully")
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Test@123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtils.generateToken(eq("testuser"), any(Map.class)))
                .thenReturn("access-token-123");
        when(jwtUtils.generateRefreshToken("testuser")).thenReturn("refresh-token-456");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-456");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        // Verify
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("Test@123");
        verify(userRepository).save(any(User.class));
        verify(jwtUtils).generateToken(eq("testuser"), any(Map.class));
        verify(jwtUtils).generateRefreshToken("testuser");
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void testRegister_DuplicateUsername_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("username")
                .hasMessageContaining("testuser");

        // Verify password was never encoded and user never saved
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegister_DuplicateEmail_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("email")
                .hasMessageContaining("test@example.com");

        verify(userRepository, never()).save(any());
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should login user successfully")
    void testLogin_Success() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateToken(eq("testuser"), any(Map.class)))
                .thenReturn("access-token-123");
        when(jwtUtils.generateRefreshToken("testuser")).thenReturn("refresh-token-456");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-456");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw exception for wrong password")
    void testLogin_WrongPassword_ThrowsException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");

        verify(userRepository, never()).findByUsername(any());
        verify(jwtUtils, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when user not found after authentication")
    void testLogin_UserNotFound_ThrowsException() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("testuser");
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Test
    @DisplayName("Should refresh token successfully")
    void testRefreshToken_Success() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(jwtUtils.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtUtils.extractUsername("valid-refresh-token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateToken(eq("testuser"), any(Map.class)))
                .thenReturn("new-access-token");

        // Act
        AuthResponse response = authService.refreshToken(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("valid-refresh-token");
        assertThat(response.getUsername()).isEqualTo("testuser");

        verify(jwtUtils).validateToken("valid-refresh-token");
        verify(jwtUtils).extractUsername("valid-refresh-token");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw exception for invalid refresh token")
    void testRefreshToken_InvalidToken_ThrowsException() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");
        when(jwtUtils.validateToken("invalid-token")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid or expired refresh token");

        verify(jwtUtils, never()).extractUsername(any());
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found for refresh token")
    void testRefreshToken_UserNotFound_ThrowsException() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(jwtUtils.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtUtils.extractUsername("valid-refresh-token")).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("nonexistent");
    }
}

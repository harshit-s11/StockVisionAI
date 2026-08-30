package com.stockvision.service;

import com.stockvision.dto.AuthResponse;
import com.stockvision.dto.LoginRequest;
import com.stockvision.dto.RegisterRequest;
import com.stockvision.entity.PortfolioEntity;
import com.stockvision.entity.Role;
import com.stockvision.entity.UserEntity;
import com.stockvision.exception.UserAlreadyExistsException;
import com.stockvision.repository.PortfolioRepository;
import com.stockvision.repository.UserRepository;
import com.stockvision.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, portfolioRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("Should successfully register user and provision initial portfolio with 100,000 cash")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("john_doe", "john@example.com", "SecurePass123!");

        when(userRepository.existsByUsernameIgnoreCase("john_doe")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed_pass");

        UUID userId = UUID.randomUUID();
        UserEntity savedUser = new UserEntity("john_doe", "john@example.com", "hashed_pass", Role.ROLE_USER);
        savedUser.setId(userId);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        PortfolioEntity savedPortfolio = new PortfolioEntity(savedUser, new BigDecimal("100000.00"));
        savedPortfolio.setId(UUID.randomUUID());
        when(portfolioRepository.save(any(PortfolioEntity.class))).thenReturn(savedPortfolio);

        when(jwtTokenProvider.generateToken(eq(userId), eq("john_doe"), eq(Role.ROLE_USER))).thenReturn("mock.jwt.token");
        when(jwtTokenProvider.getExpirationTimeSeconds()).thenReturn(86400L);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getUser().getUsername()).isEqualTo("john_doe");
        assertThat(response.getUser().getEmail()).isEqualTo("john@example.com");
        assertThat(response.getUser().getRole()).isEqualTo("USER");
        verify(userRepository).save(any(UserEntity.class));
        verify(portfolioRepository).save(any(PortfolioEntity.class));
    }

    @Test
    @DisplayName("Should propagate exception when portfolio creation fails (transaction rollback boundary)")
    void testRegisterPortfolioCreationFailure() {
        RegisterRequest request = new RegisterRequest("rollback_user", "rollback@example.com", "SecurePass123!");

        when(userRepository.existsByUsernameIgnoreCase("rollback_user")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("rollback@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed_pass");

        UserEntity savedUser = new UserEntity("rollback_user", "rollback@example.com", "hashed_pass", Role.ROLE_USER);
        savedUser.setId(UUID.randomUUID());
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        when(portfolioRepository.save(any(PortfolioEntity.class)))
            .thenThrow(new RuntimeException("Database error during portfolio creation"));

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Database error during portfolio creation");
    }

    @Test
    @DisplayName("Should reject registration when username is already taken")
    void testRegisterDuplicateUsername() {
        RegisterRequest request = new RegisterRequest("taken_user", "taken@example.com", "SecurePass123!");
        when(userRepository.existsByUsernameIgnoreCase("taken_user")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessageContaining("Username 'taken_user' is already taken");

        verify(userRepository, never()).save(any());
        verify(portfolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject registration when email is already registered")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("new_user", "existing@example.com", "SecurePass123!");
        when(userRepository.existsByUsernameIgnoreCase("new_user")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessageContaining("Email 'existing@example.com' is already registered");

        verify(userRepository, never()).save(any());
        verify(portfolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("valid_user", "CorrectPassword123!");
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("valid_user", "valid@example.com", "hashed_password", Role.ROLE_USER);
        user.setId(userId);

        when(userRepository.findByUsernameIgnoreCase("valid_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CorrectPassword123!", "hashed_password")).thenReturn(true);

        when(jwtTokenProvider.generateToken(eq(userId), eq("valid_user"), eq(Role.ROLE_USER))).thenReturn("mock.jwt.token");
        when(jwtTokenProvider.getExpirationTimeSeconds()).thenReturn(86400L);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getUser().getUsername()).isEqualTo("valid_user");
        assertThat(response.getUser().getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should reject login with incorrect password")
    void testLoginIncorrectPassword() {
        LoginRequest request = new LoginRequest("valid_user", "WrongPassword!");
        UserEntity user = new UserEntity("valid_user", "valid@example.com", "hashed_password", Role.ROLE_USER);

        when(userRepository.findByUsernameIgnoreCase("valid_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Invalid username or password");
    }

    @Test
    @DisplayName("Should reject login with unknown username")
    void testLoginUnknownUsername() {
        LoginRequest request = new LoginRequest("unknown_user", "AnyPassword!");
        when(userRepository.findByUsernameIgnoreCase("unknown_user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Invalid username or password");
    }
}
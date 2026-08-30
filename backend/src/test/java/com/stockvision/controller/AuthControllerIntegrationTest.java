package com.stockvision.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockvision.dto.LoginRequest;
import com.stockvision.dto.RegisterRequest;
import com.stockvision.entity.PortfolioEntity;
import com.stockvision.entity.Role;
import com.stockvision.entity.UserEntity;
import com.stockvision.repository.PortfolioRepository;
import com.stockvision.repository.UserRepository;
import com.stockvision.security.JwtProperties;
import com.stockvision.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should create user and initial portfolio and return 201 with UserResponse (id, username, email, role)")
    void testRegisterEndpointSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("sarah_connor", "sarah@example.com", "SecurePassword123!");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.id").isNotEmpty())
            .andExpect(jsonPath("$.user.username").value("sarah_connor"))
            .andExpect(jsonPath("$.user.email").value("sarah@example.com"))
            .andExpect(jsonPath("$.user.role").value("USER"))
            .andExpect(jsonPath("$.user.cashBalance").doesNotExist())
            .andExpect(jsonPath("$.user.createdAt").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 409 Conflict on duplicate username")
    void testRegisterDuplicateUsername() throws Exception {
        UserEntity existing = userRepository.save(new UserEntity("duplicate_user", "user1@example.com", "hashed", Role.ROLE_USER));
        portfolioRepository.save(new PortfolioEntity(existing, new BigDecimal("100000.00")));

        RegisterRequest request = new RegisterRequest("DUPLICATE_USER", "different@example.com", "Password123!");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value(containsString("already taken")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 400 Bad Request on validation failure")
    void testRegisterValidationFailure() throws Exception {
        RegisterRequest request = new RegisterRequest("ab", "invalid-email", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.validationErrors.username").isNotEmpty())
            .andExpect(jsonPath("$.validationErrors.email").isNotEmpty())
            .andExpect(jsonPath("$.validationErrors.password").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 OK with JWT token on valid credentials")
    void testLoginSuccess() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("login_trader", "trader@example.com",
            passwordEncoder.encode("MyPassword123!"), Role.ROLE_USER));
        portfolioRepository.save(new PortfolioEntity(user, new BigDecimal("100000.00")));

        LoginRequest request = new LoginRequest("login_trader", "MyPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.username").value("login_trader"))
            .andExpect(jsonPath("$.user.role").value("USER"))
            .andExpect(jsonPath("$.user.cashBalance").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 401 Unauthorized on invalid password")
    void testLoginInvalidPassword() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("login_trader2", "trader2@example.com",
            passwordEncoder.encode("CorrectPassword123!"), Role.ROLE_USER));
        portfolioRepository.save(new PortfolioEntity(user, new BigDecimal("100000.00")));

        LoginRequest request = new LoginRequest("login_trader2", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me should return only id, username, email, role when valid Bearer token provided")
    void testGetAuthMeSuccess() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("me_user", "me@example.com", "pass", Role.ROLE_USER));
        portfolioRepository.save(new PortfolioEntity(user, new BigDecimal("100000.00")));

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().toString()))
            .andExpect(jsonPath("$.username").value("me_user"))
            .andExpect(jsonPath("$.email").value("me@example.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.cashBalance").doesNotExist())
            .andExpect(jsonPath("$.createdAt").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me should return 401 Unauthorized when no token is provided")
    void testGetAuthMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me should return 401 Unauthorized when invalid token is provided")
    void testGetAuthMeWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer invalid.token.payload"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me should return 401 Unauthorized when expired token is provided")
    void testGetAuthMeWithExpiredToken() throws Exception {
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970337336763979244226452948404D6351655468576D5A7134743777217A25432A");
        expiredProps.setExpirationMs(-1000L); // expired 1s ago

        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);
        expiredProvider.init();

        String expiredToken = expiredProvider.generateToken(UUID.randomUUID(), "expired_user", Role.ROLE_USER);

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + expiredToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }
}
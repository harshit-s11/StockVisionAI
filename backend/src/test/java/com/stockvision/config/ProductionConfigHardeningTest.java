package com.stockvision.config;

import com.stockvision.controller.HealthController;
import com.stockvision.dto.ApiErrorResponse;
import com.stockvision.entity.PortfolioEntity;
import com.stockvision.entity.Role;
import com.stockvision.entity.UserEntity;
import com.stockvision.exception.GlobalExceptionHandler;
import com.stockvision.repository.PortfolioRepository;
import com.stockvision.repository.UserRepository;
import com.stockvision.security.JwtProperties;
import com.stockvision.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductionConfigHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthController healthController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Security: JWT Provider must fail fast on missing or blank secret")
    void testJwtFailsFastOnMissingSecret() {
        JwtProperties emptyProps = new JwtProperties();
        emptyProps.setSecret("");
        JwtTokenProvider provider = new JwtTokenProvider(emptyProps);

        assertThatThrownBy(provider::init)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET must be configured");
    }

    @Test
    @DisplayName("Security: JWT Provider must fail fast on weak (< 256-bit) secret")
    void testJwtFailsFastOnWeakSecret() {
        JwtProperties weakProps = new JwtProperties();
        weakProps.setSecret("weak-16-byte-sec");
        JwtTokenProvider provider = new JwtTokenProvider(weakProps);

        assertThatThrownBy(provider::init)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT secret is too weak");
    }

    @Test
    @DisplayName("Observability: Health endpoint must not expose database passwords, secrets, or internal configs")
    void testHealthEndpointSafe() throws Exception {
        ResponseEntity<Map<String, Object>> response = healthController.getHealth();
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();

        assertThat(body).containsKey("status");
        assertThat(body.get("status")).isEqualTo("UP");
        assertThat(body).containsKey("service");
        assertThat(body).containsKey("timestamp");

        // Ensure no sensitive keys exist in health output
        assertThat(body).doesNotContainKeys("password", "secret", "datasource", "apiKey", "gemini");

        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.secret").doesNotExist());
    }

    @Test
    @DisplayName("Security: Global Exception Handler must sanitize unexpected server errors and not leak stack trace")
    void testGlobalExceptionHandlerSanitizesInternalErrors() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        Exception simulatedInternalError = new RuntimeException("Sensitive database connection details: jdbc:postgresql://db:5432/stockvision?password=secret");

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleGeneralException(simulatedInternalError, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(response.getBody().getMessage()).doesNotContain("password");
        assertThat(response.getBody().getMessage()).doesNotContain("jdbc:postgresql");
    }

    @Test
    @DisplayName("Robustness: Malformed or unparseable JSON body must return 400 Bad Request instead of 500")
    void testMalformedJsonRequestBodyReturns400BadRequest() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("json_user", "json@example.com", "pass", Role.ROLE_USER));
        portfolioRepository.save(new PortfolioEntity(user, new BigDecimal("100000.00")));
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        // Malformed JSON (unquoted keys or invalid syntax)
        mockMvc.perform(post("/api/v1/trades/sell")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{symbol:TCS,quantity:1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(containsString("Malformed JSON request body")));
    }

    @Test
    @DisplayName("Robustness: Unsupported HTTP method must return 405 Method Not Allowed instead of 500")
    void testUnsupportedHttpMethodReturns405() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("http_user", "http@example.com", "pass", Role.ROLE_USER));
        portfolioRepository.save(new PortfolioEntity(user, new BigDecimal("100000.00")));
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        mockMvc.perform(patch("/api/v1/trades/sell")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\":\"TCS\",\"quantity\":1}"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405))
            .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    @DisplayName("Robustness: Unsupported media type must return 415 Unsupported Media Type instead of 500")
    void testUnsupportedMediaTypeReturns415() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("media_user", "media@example.com", "pass", Role.ROLE_USER));
        portfolioRepository.save(new PortfolioEntity(user, new BigDecimal("100000.00")));
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        mockMvc.perform(post("/api/v1/trades/sell")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.TEXT_PLAIN)
                .content("symbol=TCS&quantity=1"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.status").value(415))
            .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }
}
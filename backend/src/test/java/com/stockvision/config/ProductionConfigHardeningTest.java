package com.stockvision.config;

import com.stockvision.controller.HealthController;
import com.stockvision.dto.ApiErrorResponse;
import com.stockvision.exception.GlobalExceptionHandler;
import com.stockvision.security.JwtProperties;
import com.stockvision.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductionConfigHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthController healthController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

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
}
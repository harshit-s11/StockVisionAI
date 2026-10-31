package com.stockvision.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockvision.config.GeminiProperties;
import com.stockvision.exception.GeminiResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    private GeminiProperties geminiProperties;
    private ObjectMapper objectMapper;
    private MockRestServiceServer mockServer;
    private GeminiClient geminiClient;

    @BeforeEach
    void setUp() {
        geminiProperties = new GeminiProperties();
        geminiProperties.setApiKey("test-api-key");
        geminiProperties.setModel("gemini-3.6-flash");
        geminiProperties.setMaxOutputTokens(1200);

        objectMapper = new ObjectMapper();

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        geminiClient = new GeminiClient(geminiProperties, objectMapper, builder.build());
    }

    @Test
    @DisplayName("Regression: Extracts text from ALL candidate parts completely without mid-sentence truncation")
    void testExtractsAllCandidatePartsCompletely() {
        String responseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "Your equity portfolio currently has **very low diversification**, as 100% of your invested capital is concentrated in a single stock (TCS) within" },
                      { "text": " the Information Technology sector. Diversifying across other sectors will reduce volatility." }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP",
                  "index": 0
                }
              ]
            }
            """;

        mockServer.expect(requestTo(containsString("gemini-3.6-flash:generateContent")))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(containsString("\"maxOutputTokens\":1200")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String result = geminiClient.generateText("system instruction", "user prompt");

        assertThat(result).isEqualTo(
            "Your equity portfolio currently has **very low diversification**, as 100% of your invested capital is concentrated in a single stock (TCS) within the Information Technology sector. Diversifying across other sectors will reduce volatility."
        );
        mockServer.verify();
    }

    @Test
    @DisplayName("MAX_TOKENS handling: Retries once with increased token limit when finishReason is MAX_TOKENS")
    void testRetriesOnceWhenMaxTokensReached() {
        String truncatedResponseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "Your portfolio has low diversification because" }
                    ],
                    "role": "model"
                  },
                  "finishReason": "MAX_TOKENS",
                  "index": 0
                }
              ]
            }
            """;

        String completeResponseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "Your portfolio has low diversification because 100% of capital is in TCS. Consider rebalancing into other sectors." }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP",
                  "index": 0
                }
              ]
            }
            """;

        // First call hits MAX_TOKENS (initial limit 1200)
        mockServer.expect(requestTo(containsString("gemini-3.6-flash:generateContent")))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(containsString("\"maxOutputTokens\":1200")))
            .andRespond(withSuccess(truncatedResponseJson, MediaType.APPLICATION_JSON));

        // Retry call is made with bounded increased limit Math.max(1200*2, 2048) = 2400
        mockServer.expect(requestTo(containsString("gemini-3.6-flash:generateContent")))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(containsString("\"maxOutputTokens\":2400")))
            .andRespond(withSuccess(completeResponseJson, MediaType.APPLICATION_JSON));

        String result = geminiClient.generateText("system instruction", "user prompt");

        assertThat(result).contains("Consider rebalancing into other sectors.");
        mockServer.verify();
    }

    @Test
    @DisplayName("MAX_TOKENS handling: Throws clear retryable GeminiResponseException if retry also hits MAX_TOKENS")
    void testThrowsClearErrorIfRetryAlsoHitsMaxTokens() {
        String truncatedResponseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "Truncated sentence..." }
                    ],
                    "role": "model"
                  },
                  "finishReason": "MAX_TOKENS",
                  "index": 0
                }
              ]
            }
            """;

        // Call 1
        mockServer.expect(requestTo(containsString("gemini-3.6-flash:generateContent")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(truncatedResponseJson, MediaType.APPLICATION_JSON));

        // Call 2 (retry) also truncated
        mockServer.expect(requestTo(containsString("gemini-3.6-flash:generateContent")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(truncatedResponseJson, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> geminiClient.generateText("system instruction", "user prompt"))
            .isInstanceOf(GeminiResponseException.class)
            .hasMessageContaining("MAX_TOKENS")
            .hasMessageContaining("truncated due to token limit");

        mockServer.verify();
    }

    @Test
    @DisplayName("Safety handling: Throws GeminiResponseException when finishReason is SAFETY")
    void testThrowsExceptionWhenFlaggedBySafety() {
        String safetyResponseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": []
                  },
                  "finishReason": "SAFETY",
                  "index": 0
                }
              ]
            }
            """;

        mockServer.expect(requestTo(containsString("gemini-3.6-flash:generateContent")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(safetyResponseJson, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> geminiClient.generateText("system instruction", "user prompt"))
            .isInstanceOf(GeminiResponseException.class)
            .hasMessageContaining("SAFETY");

        mockServer.verify();
    }
}

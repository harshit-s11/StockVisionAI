package com.stockvision.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockvision.config.GeminiProperties;
import com.stockvision.exception.GeminiResponseException;
import com.stockvision.exception.GeminiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final GeminiProperties geminiProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public GeminiClient(GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this(geminiProperties, objectMapper, createDefaultRestClient());
    }

    public GeminiClient(GeminiProperties geminiProperties, ObjectMapper objectMapper, RestClient restClient) {
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    private static RestClient createDefaultRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));

        return RestClient.builder()
            .requestFactory(requestFactory)
            .build();
    }

    public String generateText(String systemInstruction, String prompt) {
        int initialTokens = (geminiProperties.getMaxOutputTokens() != null && geminiProperties.getMaxOutputTokens() > 0)
            ? geminiProperties.getMaxOutputTokens()
            : 1200;
        return executeGenerateText(systemInstruction, prompt, initialTokens, 0);
    }

    private String executeGenerateText(String systemInstruction, String prompt, int maxTokens, int attempt) {
        if (!geminiProperties.isConfigured()) {
            throw new GeminiUnavailableException("Gemini API key is not configured. Set GEMINI_API_KEY environment variable.");
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" 
                + geminiProperties.getModel() + ":generateContent?key=" + geminiProperties.getApiKey();

            Map<String, Object> requestBody = new HashMap<>();
            
            if (systemInstruction != null && !systemInstruction.trim().isEmpty()) {
                requestBody.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", systemInstruction))
                ));
            }

            requestBody.put("contents", List.of(
                Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", prompt))
                )
            ));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.2);
            generationConfig.put("maxOutputTokens", maxTokens);
            requestBody.put("generationConfig", generationConfig);

            String responseJson = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);

            // Check if prompt was blocked by safety
            JsonNode promptFeedback = root.path("promptFeedback");
            if (!promptFeedback.isMissingNode() && promptFeedback.has("blockReason")) {
                String blockReason = promptFeedback.path("blockReason").asText();
                log.warn("Gemini prompt blocked: {}", blockReason);
                throw new GeminiResponseException("Gemini prompt was blocked: " + blockReason);
            }

            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new GeminiResponseException("Gemini returned an empty or malformed response");
            }

            JsonNode candidate = candidates.get(0);
            String finishReason = candidate.has("finishReason") ? candidate.path("finishReason").asText() : null;

            // Extract all text parts completely without dropping segments
            JsonNode parts = candidate.path("content").path("parts");
            StringBuilder textBuilder = new StringBuilder();
            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    if (part.has("text")) {
                        textBuilder.append(part.path("text").asText());
                    }
                }
            }
            String fullText = textBuilder.toString();

            log.info("Gemini candidate response extracted (finishReason={}, partsCount={}, textLength={}, attempt={})",
                finishReason, parts.isArray() ? parts.size() : 0, fullText.length(), attempt);

            // Handle token-limit finish reason: bounded retry on attempt 0, or clear retryable error
            if ("MAX_TOKENS".equalsIgnoreCase(finishReason)) {
                log.warn("Gemini response reached MAX_TOKENS limit (maxTokens={}, attempt={}, textLength={})",
                    maxTokens, attempt, fullText.length());

                if (attempt == 0) {
                    int retryTokens = Math.max(maxTokens * 2, 2048);
                    log.info("Performing one bounded retry for MAX_TOKENS with increased token limit ({} tokens)...", retryTokens);
                    return executeGenerateText(systemInstruction, prompt, retryTokens, 1);
                } else {
                    throw new GeminiResponseException(
                        "AI advisor response was truncated due to token limit (finishReason: MAX_TOKENS). Please try again or ask a more specific question."
                    );
                }
            }

            if ("SAFETY".equalsIgnoreCase(finishReason) || "BLOCKLIST".equalsIgnoreCase(finishReason)
                || "PROHIBITED_CONTENT".equalsIgnoreCase(finishReason) || "SPII".equalsIgnoreCase(finishReason)) {
                log.warn("Gemini candidate blocked by content filter: {}", finishReason);
                throw new GeminiResponseException("AI response was blocked by content policy: " + finishReason);
            }

            if (fullText.trim().isEmpty()) {
                throw new GeminiResponseException("Gemini returned an empty or malformed response");
            }

            return fullText;
        } catch (GeminiUnavailableException | GeminiResponseException ex) {
            throw ex;
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 429) {
                throw new GeminiUnavailableException("Gemini API rate limit or quota exceeded. Please try again shortly.");
            } else if (ex.getStatusCode().value() == 403 || ex.getStatusCode().value() == 400) {
                throw new GeminiUnavailableException("Gemini API key is invalid or unauthorized.");
            }
            throw new GeminiUnavailableException("Gemini API service error: " + ex.getStatusCode());
        } catch (Exception ex) {
            throw new GeminiUnavailableException("Failed to communicate with Gemini API: " + ex.getMessage());
        }
    }

    public float[] generateEmbedding(String text) {
        if (!geminiProperties.isConfigured()) {
            throw new GeminiUnavailableException("Gemini API key is not configured. Set GEMINI_API_KEY environment variable.");
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" 
                + geminiProperties.getEmbeddingModel() + ":embedContent?key=" + geminiProperties.getApiKey();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "models/" + geminiProperties.getEmbeddingModel());
            requestBody.put("content", Map.of(
                "parts", List.of(Map.of("text", text))
            ));
            requestBody.put("outputDimensionality", geminiProperties.getEmbeddingDimension());

            String responseJson = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode valuesNode = root.path("embedding").path("values");

            if (valuesNode.isArray()) {
                float[] embedding = new float[valuesNode.size()];
                for (int i = 0; i < valuesNode.size(); i++) {
                    embedding[i] = (float) valuesNode.get(i).asDouble();
                }
                return embedding;
            }

            throw new GeminiResponseException("Gemini returned invalid embedding format");
        } catch (GeminiUnavailableException | GeminiResponseException ex) {
            throw ex;
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 429) {
                throw new GeminiUnavailableException("Gemini API rate limit or quota exceeded. Please try again shortly.");
            } else if (ex.getStatusCode().value() == 403 || ex.getStatusCode().value() == 400) {
                throw new GeminiUnavailableException("Gemini API key is invalid or unauthorized.");
            }
            throw new GeminiUnavailableException("Failed to generate embedding with Gemini API: " + ex.getStatusCode());
        } catch (Exception ex) {
            throw new GeminiUnavailableException("Failed to generate embedding with Gemini API: " + ex.getMessage());
        }
    }
}

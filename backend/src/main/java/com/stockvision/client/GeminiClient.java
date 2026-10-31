package com.stockvision.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockvision.config.GeminiProperties;
import com.stockvision.exception.GeminiResponseException;
import com.stockvision.exception.GeminiUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private final GeminiProperties geminiProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));

        this.restClient = RestClient.builder()
            .requestFactory(requestFactory)
            .build();
    }

    public String generateText(String systemInstruction, String prompt) {
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

            requestBody.put("generationConfig", Map.of(
                "temperature", 0.2,
                "maxOutputTokens", 768
            ));

            String responseJson = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }

            throw new GeminiResponseException("Gemini returned an empty or malformed response");
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

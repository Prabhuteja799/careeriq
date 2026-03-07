package com.careeriq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Handles all vector embedding generation.
 * Uses OpenAI text-embedding-3-small (1536 dimensions).
 * Called once per resume and once per job — result stored permanently.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    @Value("${ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${ai.openai.embedding-model}")
    private String embeddingModel;

    private final WebClient webClient;

    /**
     * Generate a 1536-dimension embedding for the given text.
     * Text should be the dense "embedding_text" field from AI parse results.
     */
    public float[] embed(String text) {
        log.debug("[Embedding] Generating embedding for text ({} chars)", text.length());

        Map<String, Object> body = Map.of(
            "model", embeddingModel,
            "input", text
        );

        try {
            Map response = webClient.post()
                .uri("https://api.openai.com/v1/embeddings")
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            List<Double> embeddingList = (List<Double>) data.get(0).get("embedding");

            float[] embedding = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                embedding[i] = embeddingList.get(i).floatValue();
            }

            log.debug("[Embedding] Generated {} dimensions", embedding.length);
            return embedding;

        } catch (Exception e) {
            log.error("[Embedding] Failed to generate embedding: {}", e.getMessage());
            throw new RuntimeException("Embedding generation failed: " + e.getMessage());
        }
    }

    /**
     * Compute cosine similarity between two vectors.
     * Used by the matching engine for scoring.
     * Pure math — zero API calls.
     */
    public double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) throw new IllegalArgumentException("Vector dimensions must match");

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

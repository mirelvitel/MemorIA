package com.memoria.server.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final String OPENAI_EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";
    private static final String MODEL = "text-embedding-3-small";

    private final RestClient restClient;
    private final String apiKey;

    public EmbeddingService(@Value("${openai.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(OPENAI_EMBEDDINGS_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbeddingResponse(List<EmbeddingData> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbeddingData(List<Double> embedding) {}

    // OpenAI recommends max 2048 inputs per request
    private static final int BATCH_SIZE = 50;

    public List<float[]> embedAll(List<String> texts) {
        log.info("Embedding {} chunks via OpenAI {} (batch size {})", texts.size(), MODEL, BATCH_SIZE);

        List<float[]> allEmbeddings = new java.util.ArrayList<>(texts.size());

        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));

            EmbeddingResponse response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", MODEL,
                            "input", batch
                    ))
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (response == null || response.data() == null) {
                throw new RuntimeException("Empty response from OpenAI embeddings API");
            }

            for (EmbeddingData d : response.data()) {
                float[] arr = new float[d.embedding().size()];
                for (int j = 0; j < arr.length; j++) {
                    arr[j] = d.embedding().get(j).floatValue();
                }
                allEmbeddings.add(arr);
            }

            log.info("Embedded batch {}/{}", Math.min(i + BATCH_SIZE, texts.size()), texts.size());
        }

        log.info("Received {} embeddings, dimension={}", allEmbeddings.size(), allEmbeddings.getFirst().length);
        return allEmbeddings;
    }
}

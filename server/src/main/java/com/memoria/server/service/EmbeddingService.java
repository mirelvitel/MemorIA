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

    public List<float[]> embedAll(List<String> texts) {
        log.info("Embedding {} chunks via OpenAI {}", texts.size(), MODEL);

        EmbeddingResponse response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", MODEL,
                        "input", texts
                ))
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.data() == null) {
            throw new RuntimeException("Empty response from OpenAI embeddings API");
        }

        List<float[]> embeddings = response.data().stream()
                .map(d -> {
                    float[] arr = new float[d.embedding().size()];
                    for (int i = 0; i < arr.length; i++) {
                        arr[i] = d.embedding().get(i).floatValue();
                    }
                    return arr;
                })
                .toList();

        log.info("Received {} embeddings, dimension={}", embeddings.size(), embeddings.getFirst().length);
        return embeddings;
    }
}

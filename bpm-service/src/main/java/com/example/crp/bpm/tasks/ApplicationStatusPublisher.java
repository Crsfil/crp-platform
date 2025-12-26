package com.example.crp.bpm.tasks;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class ApplicationStatusPublisher {
    private final WebClient applicationClient;

    public ApplicationStatusPublisher(WebClient applicationClient) {
        this.applicationClient = applicationClient;
    }

    public void publishStatus(Long applicationId, String status) {
        if (applicationId == null) {
            return;
        }
        applicationClient.patch()
                .uri("/applications/{id}/status", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", status))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}

package com.example.crp.bpm.tasks;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

abstract class BaseRestTask {
    protected final WebClient kycClient;
    protected final WebClient uwClient;
    protected final WebClient pricingClient;
    protected final WebClient applicationClient;

    public BaseRestTask(WebClient kycClient, WebClient underwritingClient, WebClient pricingClient, WebClient applicationClient) {
        this.kycClient = kycClient;
        this.uwClient = underwritingClient;
        this.pricingClient = pricingClient;
        this.applicationClient = applicationClient;
    }

    protected void setStatus(DelegateExecution exec, String status) { exec.setVariable("status", status); }

    protected void pushStatusToApp(Long appId, String status) {
        applicationClient.patch()
                .uri("/applications/{id}/status", appId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", status))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}

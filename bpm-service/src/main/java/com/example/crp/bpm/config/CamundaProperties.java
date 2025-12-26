package com.example.crp.bpm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crp.camunda")
public record CamundaProperties(
        String baseUrl,
        Worker worker
) {
    public record Worker(int maxTasks, long lockDurationMs, long asyncResponseTimeoutMs) { }
}

package com.example.crp.bpm.config;

import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(CamundaProperties.class)
public class CamundaConfig {

    @Bean
    public WebClient camundaClient(WebClient.Builder builder, CamundaProperties props) {
        return builder.clone().baseUrl(props.baseUrl()).build();
    }

    @Bean
    public ExternalTaskClient externalTaskClient(CamundaProperties props) {
        CamundaProperties.Worker worker = props.worker();
        return ExternalTaskClient.create()
                .baseUrl(props.baseUrl())
                .asyncResponseTimeout(worker.asyncResponseTimeoutMs())
                .maxTasks(worker.maxTasks())
                .lockDuration(worker.lockDurationMs())
                .build();
    }
}

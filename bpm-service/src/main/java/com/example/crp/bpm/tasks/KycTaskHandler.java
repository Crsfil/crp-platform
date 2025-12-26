package com.example.crp.bpm.tasks;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class KycTaskHandler implements CamundaTaskHandler {
    private final WebClient kycClient;
    private final ApplicationStatusPublisher statusPublisher;

    public KycTaskHandler(WebClient kycClient, ApplicationStatusPublisher statusPublisher) {
        this.kycClient = kycClient;
        this.statusPublisher = statusPublisher;
    }

    @Override
    public String topic() {
        return "kyc-check";
    }

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            Long customerId = TaskVariables.getLong(task, "customerId");
            Long applicationId = TaskVariables.getLong(task, "applicationId");
            Map response = kycClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/kyc/check").queryParam("customerId", customerId).build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            String kycStatus = response == null ? "UNKNOWN" : String.valueOf(response.get("status"));
            Map<String, Object> vars = new HashMap<>();
            vars.put("kycStatus", kycStatus);
            vars.put("status", "KYC_" + kycStatus);
            service.complete(task, vars);
            statusPublisher.publishStatus(applicationId, "KYC_" + kycStatus);
        } catch (Exception ex) {
            CamundaTaskFailures.handleFailure(service, task, ex);
        }
    }
}

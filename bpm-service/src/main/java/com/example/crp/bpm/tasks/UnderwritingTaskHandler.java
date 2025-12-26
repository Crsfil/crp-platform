package com.example.crp.bpm.tasks;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class UnderwritingTaskHandler implements CamundaTaskHandler {
    private final WebClient underwritingClient;
    private final ApplicationStatusPublisher statusPublisher;

    public UnderwritingTaskHandler(WebClient underwritingClient, ApplicationStatusPublisher statusPublisher) {
        this.underwritingClient = underwritingClient;
        this.statusPublisher = statusPublisher;
    }

    @Override
    public String topic() {
        return "underwriting";
    }

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            Long customerId = TaskVariables.getLong(task, "customerId");
            Double amount = TaskVariables.getDouble(task, "amount");
            Long applicationId = TaskVariables.getLong(task, "applicationId");
            Map response = underwritingClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/underwriting/score")
                            .queryParam("customerId", customerId)
                            .queryParam("amount", amount)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            String decision = response == null ? "UNKNOWN" : String.valueOf(response.get("decision"));
            Map<String, Object> vars = new HashMap<>();
            vars.put("uwDecision", decision);
            vars.put("status", "UW_" + decision);
            service.complete(task, vars);
            statusPublisher.publishStatus(applicationId, "UW_" + decision);
        } catch (Exception ex) {
            CamundaTaskFailures.handleFailure(service, task, ex);
        }
    }
}

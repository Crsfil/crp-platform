package com.example.crp.bpm.tasks;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class AgreementRestructureHandler implements CamundaTaskHandler {
    private final WebClient agreementClient;

    public AgreementRestructureHandler(WebClient agreementClient) {
        this.agreementClient = agreementClient;
    }

    @Override
    public String topic() {
        return "agreement-restructure";
    }

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            Long agreementId = TaskVariables.getLong(task, "agreementId");
            Integer termMonths = TaskVariables.getInt(task, "newTermMonths");
            Double rateAnnualPct = TaskVariables.getDouble(task, "rateAnnualPct");
            Double newPayment = TaskVariables.getDouble(task, "newPayment");
            Map<String, Object> payload = Map.of(
                    "termMonths", termMonths,
                    "rateAnnualPct", rateAnnualPct,
                    "newPayment", newPayment
            );
            Map response = agreementClient.patch()
                    .uri("/agreements/{id}/restructure", agreementId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Map<String, Object> vars = new HashMap<>();
            vars.put("agreementUpdated", true);
            vars.put("agreementResult", response);
            service.complete(task, vars);
        } catch (Exception ex) {
            CamundaTaskFailures.handleFailure(service, task, ex);
        }
    }
}

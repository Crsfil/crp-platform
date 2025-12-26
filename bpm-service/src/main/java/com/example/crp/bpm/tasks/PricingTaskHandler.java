package com.example.crp.bpm.tasks;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class PricingTaskHandler implements CamundaTaskHandler {
    private final WebClient pricingClient;
    private final ApplicationStatusPublisher statusPublisher;

    public PricingTaskHandler(WebClient pricingClient, ApplicationStatusPublisher statusPublisher) {
        this.pricingClient = pricingClient;
        this.statusPublisher = statusPublisher;
    }

    @Override
    public String topic() {
        return "pricing";
    }

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            Double amount = TaskVariables.getDouble(task, "amount");
            Integer term = TaskVariables.getInt(task, "termMonths");
            Double rate = TaskVariables.getDouble(task, "rateAnnualPct");
            Double downPaymentPct = TaskVariables.getDouble(task, "downPaymentPct");
            Long applicationId = TaskVariables.getLong(task, "applicationId");
            Map rule = null;
            Object ruleVar = task.getVariable("productRule");
            if (ruleVar instanceof Map) {
                rule = (Map) ruleVar;
            }
            if (rate == null && rule != null && rule.get("rateAnnualPct") != null) {
                rate = Double.parseDouble(rule.get("rateAnnualPct").toString());
            }
            if (downPaymentPct == null && rule != null && rule.get("downPaymentPct") != null) {
                downPaymentPct = Double.parseDouble(rule.get("downPaymentPct").toString());
            }
            double financedAmount = amount == null ? 0.0 : amount * (1.0 - (downPaymentPct == null ? 0.0 : downPaymentPct));
            Map response = pricingClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/pricing/calc")
                            .queryParam("amount", financedAmount)
                            .queryParam("termMonths", term)
                            .queryParam("rateAnnualPct", rate)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Map<String, Object> vars = new HashMap<>();
            vars.put("pricing", response);
            vars.put("financedAmount", financedAmount);
            vars.put("status", "PRICED");
            service.complete(task, vars);
            statusPublisher.publishStatus(applicationId, "PRICED");
        } catch (Exception ex) {
            CamundaTaskFailures.handleFailure(service, task, ex);
        }
    }
}

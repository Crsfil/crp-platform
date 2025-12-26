package com.example.crp.bpm.tasks;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class ScheduleRecalcHandler implements CamundaTaskHandler {
    private final WebClient scheduleClient;

    public ScheduleRecalcHandler(WebClient scheduleClient) {
        this.scheduleClient = scheduleClient;
    }

    @Override
    public String topic() {
        return "schedule-recalc";
    }

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            Long agreementId = TaskVariables.getLong(task, "agreementId");
            Double principal = TaskVariables.getDouble(task, "outstandingPrincipal");
            Integer termMonths = TaskVariables.getInt(task, "newTermMonths");
            Double rateAnnualPct = TaskVariables.getDouble(task, "rateAnnualPct");
            Map response = scheduleClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/schedule/restructure")
                            .queryParam("agreementId", agreementId)
                            .queryParam("amount", principal)
                            .queryParam("termMonths", termMonths)
                            .queryParam("rateAnnualPct", rateAnnualPct)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Map<String, Object> vars = new HashMap<>();
            vars.put("scheduleRecalculated", true);
            vars.put("scheduleResult", response);
            service.complete(task, vars);
        } catch (Exception ex) {
            CamundaTaskFailures.handleFailure(service, task, ex);
        }
    }
}

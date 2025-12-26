package com.example.crp.bpm.tasks;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RestructureReviewHandler implements CamundaTaskHandler {
    private static final Logger log = LoggerFactory.getLogger(RestructureReviewHandler.class);

    @Override
    public String topic() {
        return "restructure-review";
    }

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            Long agreementId = TaskVariables.getLong(task, "agreementId");
            Double marginPct = TaskVariables.getDouble(task, "marginPct");
            log.info("Restructuring requires manual review: agreementId={}, marginPct={}", agreementId, marginPct);
            service.complete(task, Map.of("reviewRequired", true));
        } catch (Exception ex) {
            CamundaTaskFailures.handleFailure(service, task, ex);
        }
    }
}

package com.example.crp.bpm.tasks;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RestructureNotifyHandler implements CamundaTaskHandler {
    private static final Logger log = LoggerFactory.getLogger(RestructureNotifyHandler.class);

    @Override
    public String topic() {
        return "restructure-notify";
    }

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            Long agreementId = TaskVariables.getLong(task, "agreementId");
            Double newPayment = TaskVariables.getDouble(task, "newPayment");
            log.info("Restructuring approved for agreementId={}, newPayment={}", agreementId, newPayment);
            service.complete(task, Map.of("notified", true));
        } catch (Exception ex) {
            CamundaTaskFailures.handleFailure(service, task, ex);
        }
    }
}

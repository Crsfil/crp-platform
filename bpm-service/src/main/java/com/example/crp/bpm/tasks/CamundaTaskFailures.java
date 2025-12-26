package com.example.crp.bpm.tasks;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;

final class CamundaTaskFailures {
    private CamundaTaskFailures() { }

    static void handleFailure(ExternalTaskService service, ExternalTask task, Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        service.handleFailure(task, message, message, 0, 5_000);
    }
}

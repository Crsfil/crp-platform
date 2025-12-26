package com.example.crp.bpm.tasks;

import org.camunda.bpm.client.task.ExternalTaskHandler;

public interface CamundaTaskHandler extends ExternalTaskHandler {
    String topic();
}

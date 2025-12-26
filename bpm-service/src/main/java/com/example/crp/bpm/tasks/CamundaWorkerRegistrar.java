package com.example.crp.bpm.tasks;

import com.example.crp.bpm.config.CamundaProperties;
import org.camunda.bpm.client.ExternalTaskClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CamundaWorkerRegistrar {
    private static final Logger log = LoggerFactory.getLogger(CamundaWorkerRegistrar.class);

    private final ExternalTaskClient client;
    private final CamundaProperties properties;
    private final List<CamundaTaskHandler> handlers;

    public CamundaWorkerRegistrar(ExternalTaskClient client,
                                  CamundaProperties properties,
                                  List<CamundaTaskHandler> handlers) {
        this.client = client;
        this.properties = properties;
        this.handlers = handlers;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerWorkers() {
        CamundaProperties.Worker worker = properties.worker();
        for (CamundaTaskHandler handler : handlers) {
            client.subscribe(handler.topic())
                    .lockDuration(worker.lockDurationMs())
                    .handler(handler)
                    .open();
            log.info("Subscribed Camunda worker to topic {}", handler.topic());
        }
    }
}

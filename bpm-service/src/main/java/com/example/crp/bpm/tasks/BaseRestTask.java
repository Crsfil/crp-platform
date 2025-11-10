package com.example.crp.bpm.tasks;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

abstract class BaseRestTask {
    protected final WebClient kycClient;
    protected final WebClient uwClient;
    protected final WebClient pricingClient;

    public BaseRestTask(WebClient kycClient, WebClient underwritingClient, WebClient pricingClient) {
        this.kycClient = kycClient;
        this.uwClient = underwritingClient;
        this.pricingClient = pricingClient;
    }

    protected void setStatus(DelegateExecution exec, String status) { exec.setVariable("status", status); }
}

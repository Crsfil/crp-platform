package com.example.crp.bpm.tasks;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

abstract class BaseRestTask {
    protected final RestClient kycClient;
    protected final RestClient uwClient;
    protected final RestClient pricingClient;
    protected final String internalApiKey;

    public BaseRestTask(@Value("${kyc.base-url:http://kyc-service:8086}") String kycBase,
                        @Value("${underwriting.base-url:http://underwriting-service:8087}") String uwBase,
                        @Value("${pricing.base-url:http://product-pricing-service:8088}") String pricingBase,
                        @Value("${security.internal-api-key:}") String internalApiKey) {
        this.kycClient = RestClient.builder().baseUrl(kycBase).build();
        this.uwClient = RestClient.builder().baseUrl(uwBase).build();
        this.pricingClient = RestClient.builder().baseUrl(pricingBase).build();
        this.internalApiKey = internalApiKey;
    }

    protected void setStatus(DelegateExecution exec, String status) { exec.setVariable("status", status); }
}


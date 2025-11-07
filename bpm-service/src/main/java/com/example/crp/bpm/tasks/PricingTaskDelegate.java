package com.example.crp.bpm.tasks;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PricingTaskDelegate extends BaseRestTask implements JavaDelegate {
    public PricingTaskDelegate(@Value("${kyc.base-url:http://kyc-service:8086}") String kycBase,
                               @Value("${underwriting.base-url:http://underwriting-service:8087}") String uwBase,
                               @Value("${pricing.base-url:http://product-pricing-service:8088}") String pricingBase,
                               @Value("${security.internal-api-key:}") String internalApiKey) {
        super(kycBase, uwBase, pricingBase, internalApiKey);
    }

    @Override
    public void execute(DelegateExecution execution) {
        Double amount = (Double) execution.getVariable("amount");
        Integer term = (Integer) execution.getVariable("termMonths");
        Double rate = (Double) execution.getVariable("rateAnnualPct");
        Map res = pricingClient.post().uri(uriBuilder -> uriBuilder
                        .path("/pricing/calc")
                        .queryParam("amount", amount)
                        .queryParam("termMonths", term)
                        .queryParam("rateAnnualPct", rate)
                        .build())
                .header("X-Internal-API-Key", internalApiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().body(Map.class);
        execution.setVariable("pricing", res);
        setStatus(execution, "PRICED");
    }
}


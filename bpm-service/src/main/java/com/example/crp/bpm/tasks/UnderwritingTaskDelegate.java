package com.example.crp.bpm.tasks;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UnderwritingTaskDelegate extends BaseRestTask implements JavaDelegate {
    public UnderwritingTaskDelegate(@Value("${kyc.base-url:http://kyc-service:8086}") String kycBase,
                                    @Value("${underwriting.base-url:http://underwriting-service:8087}") String uwBase,
                                    @Value("${pricing.base-url:http://product-pricing-service:8088}") String pricingBase,
                                    @Value("${security.internal-api-key:}") String internalApiKey) {
        super(kycBase, uwBase, pricingBase, internalApiKey);
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long customerId = (Long) execution.getVariable("customerId");
        Double amount = (Double) execution.getVariable("amount");
        Map res = uwClient.post().uri(uriBuilder -> uriBuilder
                        .path("/underwriting/score")
                        .queryParam("customerId", customerId)
                        .queryParam("amount", amount)
                        .build())
                .header("X-Internal-API-Key", internalApiKey)
                .retrieve().body(Map.class);
        if (!"APPROVED".equalsIgnoreCase(String.valueOf(res.get("decision")))) {
            setStatus(execution, "UW_REJECTED");
            throw new RuntimeException("UW rejected");
        }
        setStatus(execution, "UW_APPROVED");
    }
}


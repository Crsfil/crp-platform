package com.example.crp.bpm.tasks;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KycTaskDelegate extends BaseRestTask implements JavaDelegate {
    public KycTaskDelegate(@Value("${kyc.base-url:http://kyc-service:8086}") String kycBase,
                           @Value("${underwriting.base-url:http://underwriting-service:8087}") String uwBase,
                           @Value("${pricing.base-url:http://product-pricing-service:8088}") String pricingBase,
                           @Value("${security.internal-api-key:}") String internalApiKey) {
        super(kycBase, uwBase, pricingBase, internalApiKey);
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long customerId = (Long) execution.getVariable("customerId");
        Map res = kycClient.post().uri(uriBuilder -> uriBuilder
                        .path("/kyc/check").queryParam("customerId", customerId).build())
                .header("X-Internal-API-Key", internalApiKey)
                .retrieve().body(Map.class);
        if (!"PASSED".equalsIgnoreCase(String.valueOf(res.get("status")))) {
            setStatus(execution, "KYC_FAILED");
            throw new RuntimeException("KYC failed");
        }
        setStatus(execution, "KYC_PASSED");
    }
}


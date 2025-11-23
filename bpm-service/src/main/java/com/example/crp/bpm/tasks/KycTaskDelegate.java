package com.example.crp.bpm.tasks;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KycTaskDelegate extends BaseRestTask implements JavaDelegate {
    public KycTaskDelegate(org.springframework.web.reactive.function.client.WebClient kycClient,
                           org.springframework.web.reactive.function.client.WebClient underwritingClient,
                           org.springframework.web.reactive.function.client.WebClient pricingClient,
                           org.springframework.web.reactive.function.client.WebClient applicationClient) {
        super(kycClient, underwritingClient, pricingClient, applicationClient);
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long customerId = (Long) execution.getVariable("customerId");
        Map res = kycClient.post().uri(uriBuilder -> uriBuilder
                        .path("/kyc/check").queryParam("customerId", customerId).build())
                .retrieve().bodyToMono(Map.class).block();
        Long appId = (Long) execution.getVariable("applicationId");
        if (!"PASSED".equalsIgnoreCase(String.valueOf(res.get("status")))) {
            setStatus(execution, "KYC_FAILED");
            pushStatusToApp(appId, "KYC_FAILED");
            throw new RuntimeException("KYC failed");
        }
        setStatus(execution, "KYC_PASSED");
        pushStatusToApp(appId, "KYC_PASSED");
    }
}

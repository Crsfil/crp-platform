package com.example.crp.bpm.tasks;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UnderwritingTaskDelegate extends BaseRestTask implements JavaDelegate {
    public UnderwritingTaskDelegate(org.springframework.web.reactive.function.client.WebClient kycClient,
                                    org.springframework.web.reactive.function.client.WebClient underwritingClient,
                                    org.springframework.web.reactive.function.client.WebClient pricingClient,
                                    org.springframework.web.reactive.function.client.WebClient applicationClient) {
        super(kycClient, underwritingClient, pricingClient, applicationClient);
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long customerId = (Long) execution.getVariable("customerId");
        Double amount = (Double) execution.getVariable("amount");
        Long appId = (Long) execution.getVariable("applicationId");
        Map res = uwClient.post().uri(uriBuilder -> uriBuilder
                        .path("/underwriting/score")
                        .queryParam("customerId", customerId)
                        .queryParam("amount", amount)
                        .build())
                .retrieve().bodyToMono(Map.class).block();
        if (!"APPROVED".equalsIgnoreCase(String.valueOf(res.get("decision")))) {
            setStatus(execution, "UW_REJECTED");
            pushStatusToApp(appId, "UW_REJECTED");
            throw new RuntimeException("UW rejected");
        }
        setStatus(execution, "UW_APPROVED");
        pushStatusToApp(appId, "UW_APPROVED");
    }
}

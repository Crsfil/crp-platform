package com.example.crp.bpm.tasks;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PricingTaskDelegate extends BaseRestTask implements JavaDelegate {
    public PricingTaskDelegate(org.springframework.web.reactive.function.client.WebClient kycClient,
                               org.springframework.web.reactive.function.client.WebClient underwritingClient,
                               org.springframework.web.reactive.function.client.WebClient pricingClient) {
        super(kycClient, underwritingClient, pricingClient);
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
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(Map.class).block();
        execution.setVariable("pricing", res);
        setStatus(execution, "PRICED");
    }
}

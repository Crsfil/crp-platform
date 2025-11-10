package com.example.crp.app.web;

import com.example.crp.app.domain.Application;
import com.example.crp.app.repo.ApplicationRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applications")
public class ApplicationController {
    private final ApplicationRepository repo;
    private final WebClient kycClient, uwClient, pricingClient;

    public ApplicationController(ApplicationRepository repo,
                                 WebClient kycClient,
                                 WebClient underwritingClient,
                                 WebClient pricingClient) {
        this.repo = repo;
        this.kycClient = kycClient;
        this.uwClient = underwritingClient;
        this.pricingClient = pricingClient;
    }

    @GetMapping public List<Application> list(){ return repo.findAll(); }
    @PostMapping public Application create(@RequestBody Application a){ a.setStatus("SUBMITTED"); a.setCreatedAt(OffsetDateTime.now()); return repo.save(a);}    

    @PostMapping("/{id}/process")
    public Map<String,Object> process(@PathVariable Long id) {
        Application app = repo.findById(id).orElseThrow();
        // 1) KYC
        var kycRes = kycClient.post()
                .uri(uriBuilder -> uriBuilder.path("/kyc/check").queryParam("customerId", app.getCustomerId()).build())
                .retrieve().bodyToMono(Map.class).block();
        if (!"PASSED".equalsIgnoreCase(String.valueOf(kycRes.get("status")))) { app.setStatus("KYC_FAILED"); repo.save(app); return Map.of("status", app.getStatus()); }
        app.setStatus("KYC_PASSED"); repo.save(app);
        // 2) Underwriting
        var uwRes = uwClient.post()
                .uri(uriBuilder -> uriBuilder.path("/underwriting/score").queryParam("customerId", app.getCustomerId()).queryParam("amount", app.getAmount()).build())
                .retrieve().bodyToMono(Map.class).block();
        if (!"APPROVED".equalsIgnoreCase(String.valueOf(uwRes.get("decision")))) { app.setStatus("UW_REJECTED"); repo.save(app); return Map.of("status", app.getStatus()); }
        app.setStatus("UW_APPROVED"); repo.save(app);
        // 3) Pricing
        var pr = pricingClient.post()
                .uri(uriBuilder -> uriBuilder.path("/pricing/calc").queryParam("amount", app.getAmount()).queryParam("termMonths", app.getTermMonths()).queryParam("rateAnnualPct", app.getRateAnnualPct()).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(Map.class).block();
        app.setStatus("PRICED"); repo.save(app);
        return Map.of("status", app.getStatus(), "pricing", pr);
    }
}

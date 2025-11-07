package com.example.crp.app.web;

import com.example.crp.app.domain.Application;
import com.example.crp.app.repo.ApplicationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applications")
public class ApplicationController {
    private final ApplicationRepository repo;
    private final RestClient kycClient, uwClient, pricingClient;
    private final String internalApiKey;

    public ApplicationController(ApplicationRepository repo,
                                 @Value("${kyc.base-url:http://kyc-service:8086}") String kycBase,
                                 @Value("${underwriting.base-url:http://underwriting-service:8087}") String uwBase,
                                 @Value("${pricing.base-url:http://product-pricing-service:8088}") String pricingBase,
                                 @Value("${security.internal-api-key:}") String internalApiKey) {
        this.repo = repo; this.internalApiKey = internalApiKey;
        this.kycClient = RestClient.builder().baseUrl(kycBase).build();
        this.uwClient = RestClient.builder().baseUrl(uwBase).build();
        this.pricingClient = RestClient.builder().baseUrl(pricingBase).build();
    }

    @GetMapping public List<Application> list(){ return repo.findAll(); }
    @PostMapping public Application create(@RequestBody Application a){ a.setStatus("SUBMITTED"); a.setCreatedAt(OffsetDateTime.now()); return repo.save(a);}    

    @PostMapping("/{id}/process")
    public Map<String,Object> process(@PathVariable Long id) {
        Application app = repo.findById(id).orElseThrow();
        // 1) KYC
        var kycRes = kycClient.post().uri(uriBuilder -> uriBuilder.path("/kyc/check").queryParam("customerId", app.getCustomerId()).build())
                .header("X-Internal-API-Key", internalApiKey)
                .retrieve().body(Map.class);
        if (!"PASSED".equalsIgnoreCase(String.valueOf(kycRes.get("status")))) { app.setStatus("KYC_FAILED"); repo.save(app); return Map.of("status", app.getStatus()); }
        app.setStatus("KYC_PASSED"); repo.save(app);
        // 2) Underwriting
        var uwRes = uwClient.post().uri(uriBuilder -> uriBuilder.path("/underwriting/score").queryParam("customerId", app.getCustomerId()).queryParam("amount", app.getAmount()).build())
                .header("X-Internal-API-Key", internalApiKey)
                .retrieve().body(Map.class);
        if (!"APPROVED".equalsIgnoreCase(String.valueOf(uwRes.get("decision")))) { app.setStatus("UW_REJECTED"); repo.save(app); return Map.of("status", app.getStatus()); }
        app.setStatus("UW_APPROVED"); repo.save(app);
        // 3) Pricing
        var pr = pricingClient.post().uri(uriBuilder -> uriBuilder.path("/pricing/calc").queryParam("amount", app.getAmount()).queryParam("termMonths", app.getTermMonths()).queryParam("rateAnnualPct", app.getRateAnnualPct()).build())
                .header("X-Internal-API-Key", internalApiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().body(Map.class);
        app.setStatus("PRICED"); repo.save(app);
        return Map.of("status", app.getStatus(), "pricing", pr);
    }
}


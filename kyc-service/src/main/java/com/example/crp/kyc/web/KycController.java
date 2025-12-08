package com.example.crp.kyc.web;

import com.example.crp.kyc.service.ExternalKycClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/kyc")
public class KycController {

    private final ExternalKycClient externalKycClient;

    public KycController(ExternalKycClient externalKycClient) {
        this.externalKycClient = externalKycClient;
    }

    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> check(@RequestParam Long customerId) {
        ExternalKycClient.Decision decision = externalKycClient.check(customerId);
        return ResponseEntity.ok(Map.of(
                "customerId", decision.customerId(),
                "status", decision.status()
        ));
    }
}


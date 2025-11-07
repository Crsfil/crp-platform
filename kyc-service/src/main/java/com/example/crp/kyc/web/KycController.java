package com.example.crp.kyc.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/kyc")
public class KycController {
    @PostMapping("/check")
    public ResponseEntity<Map<String,Object>> check(@RequestParam Long customerId) {
        // stub: always pass; extend with external checks
        return ResponseEntity.ok(Map.of("customerId", customerId, "status", "PASSED"));
    }
}


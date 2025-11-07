package com.example.crp.underwriting.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/underwriting")
public class UnderwritingController {
    @PostMapping("/score")
    public ResponseEntity<Map<String,Object>> score(@RequestParam Long customerId, @RequestParam(required = false) Double amount) {
        // stub: simple rule — approve if amount <= 10_000_000
        boolean approved = amount == null || amount <= 10_000_000;
        return ResponseEntity.ok(Map.of("customerId", customerId, "decision", approved ? "APPROVED" : "REJECTED"));
    }
}


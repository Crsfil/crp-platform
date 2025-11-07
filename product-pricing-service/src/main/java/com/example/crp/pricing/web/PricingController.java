package com.example.crp.pricing.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/pricing")
public class PricingController {
    @PostMapping("/calc")
    public ResponseEntity<Map<String,Object>> calc(@RequestParam double amount, @RequestParam int termMonths, @RequestParam double rateAnnualPct) {
        double r = rateAnnualPct/12.0/100.0; int n = termMonths; double A = (r==0)? amount/n : amount * (r * Math.pow(1+r,n)) / (Math.pow(1+r,n)-1);
        List<Map<String,Object>> schedule = new ArrayList<>(); double balance = amount;
        for(int i=1;i<=n;i++){ double interest = balance * r; double principal = A - interest; balance = Math.max(0, balance - principal);
            schedule.add(Map.of("period", i, "payment", round(A), "interest", round(interest), "principal", round(principal), "balance", round(balance)));
        }
        Map<String,Object> res = new LinkedHashMap<>(); res.put("payment", round(A)); res.put("schedule", schedule);
        return ResponseEntity.ok(res);
    }
    private double round(double v){ return Math.round(v*100.0)/100.0; }
}


package com.example.crp.schedule.web;

import com.example.crp.schedule.domain.ScheduleItem;
import com.example.crp.schedule.repo.ScheduleRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {
    private final ScheduleRepository repo;
    private final WebClient pricingClient;

    public ScheduleController(ScheduleRepository repo, WebClient pricingClient) {
        this.repo = repo; this.pricingClient = pricingClient;
    }

    @PostMapping("/generate")
    public List<ScheduleItem> generate(@RequestParam Long agreementId,
                                       @RequestParam double amount,
                                       @RequestParam int termMonths,
                                       @RequestParam double rateAnnualPct) {
        Map res = pricingClient.post().uri(uriBuilder -> uriBuilder
                        .path("/pricing/calc")
                        .queryParam("amount", amount)
                        .queryParam("termMonths", termMonths)
                        .queryParam("rateAnnualPct", rateAnnualPct)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(Map.class).block();
        List<Map<String,Object>> schedule = (List<Map<String,Object>>) res.get("schedule");
        List<ScheduleItem> out = new ArrayList<>();
        LocalDate base = LocalDate.now().plusMonths(1);
        for (int i=0;i<schedule.size();i++){
            Map m = schedule.get(i);
            ScheduleItem si = new ScheduleItem();
            si.setAgreementId(agreementId);
            si.setDueDate(base.plusMonths(i));
            si.setAmount(((Number)m.get("payment")).doubleValue());
            si.setStatus("PLANNED");
            out.add(repo.save(si));
        }
        return out;
    }

    @GetMapping("/due")
    public List<ScheduleItem> due(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return repo.findByDueDateAndStatus(date, "PLANNED");
    }

    @PostMapping("/{id}/markInvoiced")
    public Map<String,Object> markInvoiced(@PathVariable Long id) {
        ScheduleItem si = repo.findById(id).orElseThrow();
        si.setStatus("INVOICED");
        repo.save(si);
        return Map.of("status","INVOICED");
    }
}
